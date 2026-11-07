package database

import java.util.UUID

import database.MyPostgresProfile.api.*
import database.MyPostgresProfile.MyAPI.getJson
import database.MyPostgresProfile.MyAPI.setUUIDArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json

/** Integration tests of `exams.merge_exams` and `exams.repoint_exam_refs`. */
final class MergeExamsSpec extends ExamFixtures {

  protected def idPrefix = 5

  private val po        = "exam_fixture_merge_po"
  private val emptyPo   = "exam_fixture_merge_po2" // config without registrationOverrides
  private val untouched = "exam_fixture_merge_po3" // config without references to the sources

  private val analysis = id(1)
  private val algebra  = id(2)
  private val project  = id(3)
  private val target   = id(101)
  private val source   = id(102) // shares analysis with the target
  private val source2  = id(103)
  private val other    = id(104)
  private val sources  = List(source, source2)

  private val examList = id(301)
  private val plan     = id(401)

  private def listExam(exam: UUID, name: String) =
    Json.obj("exam" -> exam, "name" -> name, "registrationPeriods" -> Json.obj())

  private def excluded(module: UUID, exam: UUID) = Json.obj("module" -> module, "exam" -> exam)

  private def registrationOverride(module: UUID, exam: UUID, phase: String, start: String) =
    Json.obj("module" -> module, "exam" -> exam, "phase" -> phase, "start" -> start, "end" -> "2098-01-31")

  private def listEntry(entry: UUID, module: UUID, exams: JsValue) =
    sqlu"""INSERT INTO exams.exam_list_entry
             (id, exam_list, module, module_title, module_abbrev, mandatory, po, assessment_methods,
              first_examiner, second_examiner, exam_phases, exams)
           SELECT ${entry.toString}::uuid, ${examList.toString}::uuid, ${module.toString}::uuid, 'Title', 'T', TRUE,
                  '[]'::jsonb, '{}', examiner.id, examiner.id, '{wise_1}', ${exams.toString}::jsonb
           FROM (SELECT id FROM core.identity ORDER BY id LIMIT 1) examiner"""

  private val fixtures = DBIO.seq(
    insertPo(po),
    insertPo(emptyPo),
    insertPo(untouched),
    insertModule(analysis, "Analysis"),
    insertModule(algebra, "Algebra"),
    insertModule(project, "Projekt"),
    exam(target, "Klausur", analysis),
    exam(source, "Klausur alt", analysis, algebra),
    exam(source2, "Projekt", project),
    exam(other, "Other", algebra),
    sqlu"""INSERT INTO exams.exam_list (id, po, semester, date, url)
           VALUES (${examList.toString}::uuid, $po, 'wise_2097', DATE '2097-10-01', 'fixture.pdf')""",
    listEntry(id(311), analysis, Json.arr(listExam(target, "Klausur"), listExam(source, "Klausur alt"))),
    listEntry(
      id(312),
      project,
      Json.arr(listExam(source2, "Projekt"), listExam(other, "Other"), listExam(source, "Klausur alt"))
    ),
    listEntry(id(313), algebra, Json.arr(listExam(other, "Other"))),
    insertConfig(
      po,
      Json.obj(
        "semester"              -> "wise_2097",
        "examPhases"            -> List("wise_1", "wise_2"),
        "excludedExams"         -> Json.arr(excluded(analysis, source), excluded(analysis, target), excluded(algebra, source)),
        "registrationOverrides" -> Json.arr(
          registrationOverride(analysis, source, "wise_1", "2098-01-01"),
          registrationOverride(analysis, target, "wise_1", "2098-01-15"),
          registrationOverride(analysis, source, "wise_2", "2098-01-01"),
          registrationOverride(algebra, other, "wise_1", "2098-01-01")
        )
      )
    ),
    insertConfig(emptyPo, Json.obj("excludedExams" -> Json.arr(excluded(project, source2)))),
    insertConfig(untouched, Json.obj("excludedExams" -> Json.arr(excluded(algebra, other)))),
    examPlan(plan, "wise_2097"),
    entryDraft(id(411), plan, source, 9, 11),
    entryDraft(id(412), plan, other, 12, 13)
  )

  private def merge(name: String) =
    sql"SELECT exams.merge_exams(${target.toString}::uuid, $sources::uuid[], $name)".as[Option[String]]

  private val assignments: DBIO[Map[UUID, Set[UUID]]] =
    sql"""SELECT exam::text, module::text FROM exams.exam_module
          WHERE exam = ANY (${List(target, source, source2, other)}::uuid[])"""
      .as[(String, String)]
      .map(_.groupMap(r => UUID.fromString(r._1))(r => UUID.fromString(r._2)).view.mapValues(_.toSet).toMap)

  private val names: DBIO[Map[UUID, String]] =
    sql"SELECT id::text, name FROM exams.exam WHERE id = ANY (${List(target, source, source2, other)}::uuid[])"
      .as[(String, String)]
      .map(_.map((id, name) => UUID.fromString(id) -> name).toMap)

  /** Physical row versions, which change on every write. */
  private def version(table: String, key: String, value: String) =
    sql"SELECT ctid::text FROM #$table WHERE #$key = $value".as[String].head

  private def entryExams(entry: UUID) =
    sql"SELECT exams FROM exams.exam_list_entry WHERE id = ${entry.toString}::uuid".as[JsValue].head

  private def config(p: String) = sql"SELECT config FROM exams.exam_list_config WHERE po = $p".as[JsValue].head

  test("the target gets the new name and the modules of all sources, which are deleted") {
    val (assigned, named) = rollback(fixtures >> merge("Klausur neu") >> (assignments.zip(names)))

    assigned shouldBe Map(target -> Set(analysis, algebra, project), other -> Set(algebra))
    named shouldBe Map(target -> "Klausur neu", other -> "Other")
  }

  test("an unchanged name is not rewritten") {
    val (before, after) = rollback(for {
      _      <- fixtures
      before <- version("exams.exam", "id::text", target.toString)
      _      <- merge("Klausur")
      after  <- version("exams.exam", "id::text", target.toString)
    } yield (before, after))

    after shouldBe before
  }

  test("published list exams point to the target, keeping the first of duplicates in order") {
    val (shared, mixed, before, after) = rollback(for {
      _      <- fixtures
      before <- version("exams.exam_list_entry", "id::text", id(313).toString)
      _      <- merge("Klausur")
      shared <- entryExams(id(311))
      mixed  <- entryExams(id(312))
      after  <- version("exams.exam_list_entry", "id::text", id(313).toString)
    } yield (shared, mixed, before, after))

    shared shouldBe Json.arr(listExam(target, "Klausur"))
    // the first occurrence keeps its name at publication
    mixed shouldBe Json.arr(listExam(target, "Projekt"), listExam(other, "Other"))
    after shouldBe before
  }

  test("exclusions and overrides point to the target, duplicates per module, exam and phase are removed") {
    val (merged, empty, before, after) = rollback(for {
      _      <- fixtures
      before <- version("exams.exam_list_config", "po", untouched)
      _      <- merge("Klausur")
      merged <- config(po)
      empty  <- config(emptyPo)
      after  <- version("exams.exam_list_config", "po", untouched)
    } yield (merged, empty, before, after))

    merged shouldBe Json.obj(
      "semester"              -> "wise_2097",
      "examPhases"            -> List("wise_1", "wise_2"),
      "excludedExams"         -> Json.arr(excluded(analysis, target), excluded(algebra, target)),
      "registrationOverrides" -> Json.arr(
        registrationOverride(analysis, target, "wise_1", "2098-01-01"),
        registrationOverride(analysis, target, "wise_2", "2098-01-01"),
        registrationOverride(algebra, other, "wise_1", "2098-01-01")
      )
    )
    empty.as[JsObject] shouldBe Json.obj(
      "excludedExams"         -> Json.arr(excluded(project, target)),
      "registrationOverrides" -> Json.arr()
    )
    after shouldBe before
  }

  test("appointments and their published copies point to the target and stay published") {
    val (planned, live, pending) = rollback(for {
      _       <- fixtures
      _       <- publishExamPlan(plan)
      _       <- merge("Klausur")
      planned <- sql"""SELECT exam::text FROM schedule.exam_entry_draft
                       WHERE plan_draft = ${plan.toString}::uuid ORDER BY "start"""".as[String]
      live <- sql"""SELECT exam::text FROM schedule.exam_entry
                    WHERE source_plan_draft = ${plan.toString}::uuid ORDER BY "start"""".as[String]
      pending <- hasUnpublishedChanges(plan)
    } yield (planned, live, pending))

    planned shouldBe Vector(target.toString, other.toString)
    live shouldBe planned
    pending shouldBe false
  }
}
