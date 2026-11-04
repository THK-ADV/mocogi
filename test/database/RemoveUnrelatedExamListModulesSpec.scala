package database

import java.util.UUID

import database.MyPostgresProfile.api.*
import database.MyPostgresProfile.MyAPI.getJson
import database.MyPostgresProfile.MyAPI.setUUIDArray
import play.api.libs.json.JsArray
import play.api.libs.json.JsNull
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import service.artifact.examlist.ExamListConfig

/** Integration tests of `exams.remove_unrelated_exam_list_modules`. */
final class RemoveUnrelatedExamListModulesSpec extends ExamFixtures {

  protected def idPrefix = 2

  private val po      = "exam_fixture_remove_po"
  private val otherPo = "exam_fixture_remove_po2"

  private val generic  = id(999)
  private val moved    = id(1) // relates to the other PO only
  private val direct   = id(2) // relates to the PO directly
  private val parent   = id(3) // relates to the PO only via its child
  private val child    = id(4)
  private val notGiven = id(5) // unrelated, but not passed to the function

  private def excludedExam(module: UUID) = Json.obj("module" -> module, "exam" -> JsNull)

  private def registrationOverride(module: UUID) =
    Json.obj("module" -> module, "exam" -> JsNull, "phase" -> "wise_1", "start" -> "2027-01-04", "end" -> "2027-01-08")

  private def config(modules: List[UUID]) =
    Json.obj(
      "semester"              -> "wise_2026",
      "date"                  -> JsNull,
      "examPhases"            -> List("wise_1"),
      "excludeGenericModules" -> true,
      "registrationPeriods"   -> Json.obj("wise_1" -> Json.obj("start" -> "2027-01-01", "end" -> "2027-01-08")),
      "excludedModuleIds"     -> modules,
      "excludedExams"         -> modules.map(excludedExam),
      "registrationOverrides" -> modules.map(registrationOverride)
    )

  private val allModules = List(moved, direct, parent, notGiven)

  /** Stages both POs, the modules and their relations. */
  private val fixtures = DBIO.seq(
    insertPo(po),
    insertPo(otherPo),
    insertModule(generic, "Generic", generic = true),
    insertModule(moved, "Moved"),
    insertModule(direct, "Direct"),
    insertModule(parent, "Parent"),
    insertModule(child, "Child"),
    insertModule(notGiven, "Not Given"),
    relate(parent, child),
    mandatory(moved, otherPo, List(1)),
    mandatory(direct, po, List(1)),
    optional(child, po, generic, List(2))
  )

  private def remove(modules: List[UUID]): DBIO[Int] =
    sql"SELECT exams.remove_unrelated_exam_list_modules($modules)".as[Int].head

  private def configOf(po: String): DBIO[JsValue] =
    sql"SELECT config FROM exams.exam_list_config WHERE po = $po".as[JsValue].head

  /** Physical row versions of all configurations, which change on every write. */
  private val versions: DBIO[Map[String, String]] =
    sql"SELECT po, ctid::text FROM exams.exam_list_config".as[(String, String)].map(_.toMap)

  test("removes a module from the config of a PO it no longer relates to and keeps everything else") {
    val (count, updated, other, before, after) = rollback(for {
      _       <- fixtures
      _       <- insertConfig(po, config(allModules))
      _       <- insertConfig(otherPo, config(List(notGiven)))
      before  <- versions
      count   <- remove(List(moved, direct, parent))
      after   <- versions
      updated <- configOf(po)
      other   <- configOf(otherPo)
    } yield (count, updated, other, before, after))

    count shouldBe 1
    updated shouldBe config(List(direct, parent, notGiven))
    other shouldBe config(List(notGiven))
    after shouldBe before.updated(po, after(po))
    after(po) should not be before(po)
  }

  test("keeps a module that relates to the PO directly or only via a child and writes nothing") {
    val (count, updated, before, after) = rollback(for {
      _       <- fixtures
      _       <- insertConfig(po, config(allModules))
      before  <- versions
      count   <- remove(List(direct, parent))
      after   <- versions
      updated <- configOf(po)
    } yield (count, updated, before, after))

    count shouldBe 0
    updated shouldBe config(allModules)
    after shouldBe before
  }

  test("removes a child module although it relates to the PO directly, since only its parent is a candidate") {
    val (count, updated) = rollback(for {
      _       <- fixtures
      _       <- mandatory(child, po, List(1))
      _       <- insertConfig(po, config(List(child, direct)))
      count   <- remove(List(child))
      updated <- configOf(po)
    } yield (count, updated))

    count shouldBe 1
    updated shouldBe config(List(direct))
  }

  test("counts every changed config once, also if the module is referenced in several arrays") {
    val (count, updated, emptied) = rollback(for {
      _       <- fixtures
      _       <- insertConfig(po, config(List(moved, notGiven)))
      _       <- insertConfig(otherPo, config(List(direct)))
      count   <- remove(List(moved, direct))
      updated <- configOf(po)
      emptied <- configOf(otherPo)
    } yield (count, updated, emptied))

    count shouldBe 2
    updated shouldBe config(List(notGiven))
    emptied shouldBe config(Nil)
  }

  test("removes several unrelated modules from the same config in one write") {
    val (count, updated) = rollback(for {
      _       <- fixtures
      _       <- insertConfig(po, config(List(moved, direct, notGiven)))
      count   <- remove(List(moved, notGiven))
      updated <- configOf(po)
    } yield (count, updated))

    count shouldBe 1
    updated shouldBe config(List(direct))
  }

  for key <- List("excludedModuleIds", "excludedExams", "registrationOverrides") do
    test(s"finds a module referenced only in $key") {
      val base  = config(List(direct))
      val entry = key match {
        case "excludedModuleIds" => Json.toJson(moved)
        case "excludedExams"     => excludedExam(moved)
        case _                   => registrationOverride(moved)
      }
      val (count, updated) = rollback(for {
        _       <- fixtures
        _       <- insertConfig(po, base ++ Json.obj(key -> ((base \ key).as[JsArray] :+ entry)))
        count   <- remove(List(moved))
        updated <- configOf(po)
      } yield (count, updated))

      count shouldBe 1
      updated shouldBe base
    }

  test("an empty array changes nothing") {
    val (count, before, after) = rollback(for {
      _      <- fixtures
      _      <- insertConfig(po, config(allModules))
      before <- versions
      count  <- remove(Nil)
      after  <- versions
    } yield (count, before, after))

    count shouldBe 0
    after shouldBe before
  }

  test("a config without some of the arrays gets them as empty arrays and stays readable") {
    val partial          = config(List(moved, notGiven)) - "registrationOverrides" - "excludedExams"
    val (count, updated) = rollback(for {
      _       <- fixtures
      _       <- insertConfig(po, partial)
      count   <- remove(List(moved))
      updated <- configOf(po)
    } yield (count, updated))

    count shouldBe 1
    updated shouldBe partial ++ Json.obj(
      "excludedModuleIds"     -> List(notGiven),
      "excludedExams"         -> Json.arr(),
      "registrationOverrides" -> Json.arr()
    )
    updated.validate[ExamListConfig].isSuccess shouldBe true
  }
}
