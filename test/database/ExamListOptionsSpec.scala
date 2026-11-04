package database

import java.util.UUID

import database.MyPostgresProfile.api.*
import database.MyPostgresProfile.MyAPI.getJson
import play.api.libs.json.JsArray
import play.api.libs.json.JsNull
import play.api.libs.json.JsValue
import play.api.libs.json.Json

/** Integration tests of `exams.exam_list_options`. */
final class ExamListOptionsSpec extends ExamFixtures {

  protected def idPrefix = 1

  private val po = "exam_fixture_options_po"

  private val generic = id(999)

  /** Stages the PO and a generic module used as `instance_of` of optional relations. */
  private def staged[A](action: DBIO[A]): A =
    rollback(insertPo(po) >> insertModule(generic, "Generic Fixture", generic = true) >> action)

  private def options(po: String): DBIO[List[JsValue]] =
    sql"SELECT exams.exam_list_options($po)".as[JsValue].head.map(_.as[List[JsValue]])

  private def byId(options: List[JsValue]): Map[UUID, JsValue] = options.map(o => (o \ "id").as[UUID] -> o).toMap

  test("a child module is never a candidate, its PO relation makes its parent one") {
    val (parent, child) = (id(1), id(2))
    val result          = staged(
      insertModule(parent, "Parent") >> insertModule(child, "Child") >> relate(parent, child) >>
        mandatory(child, po, List(3)) >> options(po)
    )

    result.map(o => (o \ "id").as[UUID]) shouldBe List(parent)
  }

  test("exam phases of the parent are followed by the missing phases of its active related children") {
    val (parent, active1, active2, inactive, unrelated, single) = (id(1), id(2), id(3), id(4), id(5), id(6))
    val result                                                  = staged(
      insertModule(parent, "Parent", phases = List("wise_2", "sose_1")) >>
        insertModule(active1, "Active 1", phases = List("sose_1", "zz", "ab")) >>
        insertModule(active2, "Active 2", phases = List("ab", "mm")) >>
        insertModule(inactive, "Inactive", active = false, phases = List("inactive_only")) >>
        insertModule(unrelated, "Unrelated", phases = List("unrelated_only")) >>
        insertModule(single, "Single", phases = List("wise_2", "sose_1")) >>
        DBIO.seq(List(active1, active2, inactive, unrelated).map(relate(parent, _))*) >>
        DBIO.seq(List(active1, active2, inactive, single).map(mandatory(_, po, List(1)))*) >>
        options(po)
    )
    val phases = byId(result).view.mapValues(o => (o \ "examPhases").as[List[String]]).toMap

    // the child without a relation to the PO does not contribute its phases
    phases(parent) shouldBe List("wise_2", "sose_1", "ab", "mm", "zz")
    phases(single) shouldBe List("wise_2", "sose_1")
  }

  test("inactive candidates are excluded, also if an active child relates to the PO") {
    val (inactive, inactiveParent, activeChild, active) = (id(1), id(2), id(3), id(4))
    val result                                          = staged(
      insertModule(inactive, "Inactive", active = false) >>
        insertModule(inactiveParent, "Inactive Parent", active = false) >>
        insertModule(activeChild, "Active Child") >>
        insertModule(active, "Active") >>
        relate(inactiveParent, activeChild) >>
        DBIO.seq(List(inactive, activeChild, active).map(mandatory(_, po, List(1)))*) >>
        options(po)
    )

    result.map(o => (o \ "id").as[UUID]) shouldBe List(active)
  }

  test("relations of inactive children count for neither candidacy nor semester, mandatory and po") {
    val (onlyInactive, retired, parent, retiredSibling) = (id(1), id(2), id(3), id(4))
    val result                                          = staged(
      insertModule(onlyInactive, "Only Inactive") >> insertModule(retired, "Retired", active = false) >>
        insertModule(parent, "Parent") >> insertModule(retiredSibling, "Retired Sibling", active = false) >>
        relate(onlyInactive, retired) >> relate(parent, retiredSibling) >>
        mandatory(retired, po, List(1)) >> mandatory(retiredSibling, po, List(1)) >>
        optional(parent, po, generic, List(4)) >>
        options(po)
    )

    result.map(o => (o \ "id").as[UUID]) shouldBe List(parent)
    (result.head \ "semester").as[Int] shouldBe 4
    (result.head \ "mandatory").as[Boolean] shouldBe false
    (result.head \ "po" \ 0 \ "recommendedSemester").as[List[Int]] shouldBe List(4)
    (result.head \ "po").as[List[JsValue]] should have size 1
  }

  test("semester is the smallest recommended semester over all relations including those of children") {
    val (parent, child, noSemester, partlyEmpty) = (id(1), id(2), id(3), id(4))
    val result                                   = staged(
      insertModule(parent, "Parent") >> insertModule(child, "Child") >> relate(parent, child) >>
        insertModule(noSemester, "No Semester") >> insertModule(partlyEmpty, "Partly Empty") >>
        mandatory(parent, po, List(5, 4)) >> optional(child, po, generic, List(6, 2)) >>
        mandatory(noSemester, po, Nil) >> optional(noSemester, po, generic, Nil) >>
        mandatory(partlyEmpty, po, Nil) >> optional(partlyEmpty, po, generic, List(7)) >>
        options(po)
    )
    val semesters = byId(result).view.mapValues(o => (o \ "semester").as[Int]).toMap

    semesters shouldBe Map(parent -> 2, noSemester -> Int.MaxValue, partlyEmpty -> 7)
  }

  test("mandatory if any relation is mandatory, po contains each distinct relation once") {
    val (parent, child, optionalOnly) = (id(1), id(2), id(3))
    val result                        = staged(
      insertModule(parent, "Parent") >> insertModule(child, "Child") >> relate(parent, child) >>
        insertModule(optionalOnly, "Optional Only") >>
        optional(parent, po, generic, List(2)) >> mandatory(parent, po, List(1)) >> mandatory(child, po, List(1)) >>
        optional(optionalOnly, po, generic, List(2)) >> optional(optionalOnly, po, generic, List(2)) >>
        options(po)
    )
    val option                                                                = byId(result)
    def relation(mandatory: Boolean, semester: Int, instanceOf: Option[UUID]) =
      Json.obj(
        "po"                  -> po,
        "specialization"      -> JsNull,
        "recommendedSemester" -> List(semester),
        "mandatory"           -> mandatory,
        "instanceOf"          -> instanceOf
      )

    (option(parent) \ "mandatory").as[Boolean] shouldBe true
    (option(optionalOnly) \ "mandatory").as[Boolean] shouldBe false
    (option(parent) \ "po").as[List[JsValue]] should contain theSameElementsAs List(
      relation(mandatory = true, 1, None),
      relation(mandatory = false, 2, Some(generic))
    )
    (option(optionalOnly) \ "po").as[List[JsValue]] shouldBe List(relation(mandatory = false, 2, Some(generic)))
  }

  test("exam names are empty for generic modules, null without named exams and sorted by name otherwise") {
    val (genericCandidate, default, none, named) = (id(1), id(2), id(3), id(4))
    val result                                   = staged(
      insertModule(genericCandidate, "Generic", generic = true) >> insertModule(default, "Default") >>
        insertModule(none, "None") >> insertModule(named, "Named") >>
        exam(genericCandidate, Some("Stale")) >> exam(default, None) >>
        exam(named, Some("Projekt")) >> exam(named, Some("Klausur")) >>
        DBIO.seq(List(genericCandidate, default, none, named).map(mandatory(_, po, List(1)))*) >>
        options(po)
    )
    val option = byId(result)

    (option(genericCandidate) \ "isGeneric").as[Boolean] shouldBe true
    (option(default) \ "isGeneric").as[Boolean] shouldBe false
    (option(genericCandidate) \ "examNames").get shouldBe JsArray()
    (option(default) \ "examNames").get shouldBe JsNull
    (option(none) \ "examNames").get shouldBe JsNull
    (option(named) \ "examNames").as[List[String]] shouldBe List("Klausur", "Projekt")
  }

  test("assessment methods are sorted by method and empty without any") {
    val (withMethods, withoutMethods, child) = (id(1), id(2), id(3))
    val (methodA, methodB)                   = ("exam_fixture_method_a", "exam_fixture_method_b")
    val result                               = staged(
      insertAssessmentMethod(methodA) >> insertAssessmentMethod(methodB) >>
        insertModule(withMethods, "With Methods") >> insertModule(withoutMethods, "Without Methods") >>
        insertModule(child, "Child") >> relate(withoutMethods, child) >>
        assess(withMethods, methodB, Some(BigDecimal(40))) >> assess(withMethods, methodA, None) >>
        assess(child, methodA, Some(BigDecimal(100))) >>
        mandatory(withMethods, po, List(1)) >> mandatory(child, po, List(1)) >>
        options(po)
    )
    val option = byId(result)

    (option(withMethods) \ "assessmentMethods").get shouldBe Json.arr(
      Json.obj("method" -> methodA, "percentage" -> JsNull),
      Json.obj("method" -> methodB, "percentage" -> 40)
    )
    // methods of a child module are not taken over by its parent
    (option(withoutMethods) \ "assessmentMethods").get shouldBe JsArray()
  }

  test("an unknown PO or a PO without relations yields an empty array") {
    val (unknown, unrelated) = staged(options("exam_fixture_unknown_po").zip(options(po)))

    unknown shouldBe empty
    unrelated shouldBe empty
  }

  test("candidates are ordered by title, then id") {
    val (beta, alpha2, alpha1) = (id(1), id(3), id(2))
    val result                 = staged(
      insertModule(beta, "Beta") >> insertModule(alpha2, "Alpha") >> insertModule(alpha1, "Alpha") >>
        DBIO.seq(List(beta, alpha2, alpha1).map(mandatory(_, po, List(1)))*) >>
        options(po)
    )

    result.map(o => (o \ "id").as[UUID]) shouldBe List(alpha1, alpha2, beta)
  }
}
