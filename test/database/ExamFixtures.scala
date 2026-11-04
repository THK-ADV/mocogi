package database

import java.util.UUID

import scala.concurrent.ExecutionContext

import database.MyPostgresProfile.api.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import play.api.libs.json.JsValue
import play.api.libs.json.Json

/**
 * Stages exam fixtures inside a transaction that is always rolled back, so nothing persists. Rows are copied from
 * arbitrary existing ones with overridden columns, which keeps the fixtures independent of specific data.
 */
trait ExamFixtures extends AnyFunSuite, Matchers, BeforeAndAfterAll {

  given ExecutionContext = ExecutionContext.global

  override def beforeAll(): Unit = TestDb.start()

  /** Prefix of the fixture UUIDs, distinct per spec to avoid lock waits between suites running in parallel. */
  protected def idPrefix: Int

  protected def id(n: Int): UUID = UUID.fromString(f"00000000-$idPrefix%04d-4000-8000-$n%012d")

  private final case class Rollback(value: Any) extends Exception(null, null, false, false)

  /** Runs the action in a transaction that is rolled back and returns its result. */
  protected def rollback[A](action: DBIO[A]): A =
    try TestDb.runSync(action.flatMap(a => DBIO.failed(Rollback(a))).transactionally)
    catch case Rollback(a) => a.asInstanceOf[A]

  protected def insertPo(po: String): DBIO[Int] =
    sqlu"""INSERT INTO core.po
           SELECT (jsonb_populate_record(p, jsonb_build_object('id', $po))).* FROM core.po p LIMIT 1"""

  protected def insertModule(
      module: UUID,
      title: String,
      generic: Boolean = false,
      active: Boolean = true,
      phases: List[String] = Nil
  ): DBIO[Int] = {
    val columns = Json.obj(
      "id"          -> module,
      "title"       -> title,
      "abbrev"      -> title,
      "module_type" -> (if generic then "generic_module" else "module"),
      "status"      -> (if active then "active" else "inactive"),
      "exam_phases" -> phases
    )
    sqlu"""INSERT INTO modules.module
           SELECT (jsonb_populate_record(m, ${columns.toString}::jsonb)).* FROM modules.module m LIMIT 1"""
  }

  protected def relate(parent: UUID, child: UUID): DBIO[Int] =
    sqlu"INSERT INTO modules.module_relation (parent, child) VALUES (${parent.toString}::uuid, ${child.toString}::uuid)"

  private def semesterArray(semesters: List[Int]) = semesters.mkString("{", ",", "}")

  protected def mandatory(module: UUID, po: String, semesters: List[Int]): DBIO[Int] =
    sqlu"""INSERT INTO modules.module_po_mandatory (id, module, po, recommended_semester)
           VALUES (gen_random_uuid(), ${module.toString}::uuid, $po, ${semesterArray(semesters)}::int[])"""

  protected def optional(module: UUID, po: String, instanceOf: UUID, semesters: List[Int]): DBIO[Int] =
    sqlu"""INSERT INTO modules.module_po_optional (id, module, po, instance_of, part_of_catalog, recommended_semester)
           VALUES (gen_random_uuid(), ${module.toString}::uuid, $po, ${instanceOf.toString}::uuid, FALSE,
                   ${semesterArray(semesters)}::int[])"""

  protected def exam(module: UUID, name: Option[String]): DBIO[Int] =
    sqlu"INSERT INTO exams.module_exam (module, name) VALUES (${module.toString}::uuid, $name)"

  protected def insertAssessmentMethod(method: String): DBIO[Int] =
    sqlu"""INSERT INTO core.assessment_method
           SELECT (jsonb_populate_record(a, jsonb_build_object('id', $method))).* FROM core.assessment_method a LIMIT 1"""

  protected def assess(module: UUID, method: String, percentage: Option[BigDecimal]): DBIO[Int] =
    sqlu"""INSERT INTO modules.module_assessment_method (id, module, assessment_method, percentage)
           VALUES (gen_random_uuid(), ${module.toString}::uuid, $method, $percentage)"""

  protected def insertConfig(po: String, config: JsValue): DBIO[Int] =
    sqlu"INSERT INTO exams.exam_list_config (po, config) VALUES ($po, ${config.toString}::jsonb)"
}
