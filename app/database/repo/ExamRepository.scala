package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.ExamModuleTable
import database.table.ExamTable
import database.table.ModuleTable
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import slick.jdbc.JdbcProfile

/**
 * Exams and their assignments to modules with own exams. Writes lock the exam rows and the assigned module rows, which
 * serializes them with each other and with module syncs.
 */
@Singleton
final class ExamRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.MyPostgresProfile.MyAPI.getJson
  import database.MyPostgresProfile.MyAPI.getJsonOption
  import database.MyPostgresProfile.MyAPI.setJson
  import database.MyPostgresProfile.MyAPI.setUUIDArray
  import profile.api.*

  private val exams       = TableQuery[ExamTable]
  private val assignments = TableQuery[ExamModuleTable]

  /**
   * All exams and the active modules that may have own exams but have none, see `exams.get_exams` and
   * `exams.modules_without_exam`.
   */
  def all(): Future[JsValue] =
    db.run(
      sql"""select jsonb_build_object('exams', exams.get_exams(null), 'modulesWithoutExam', exams.modules_without_exam())"""
        .as[JsValue]
        .head
    )

  /** Exams of the live module for the module editor, or `None` if it does not exist, see `exams.module_exams`. */
  def moduleExams(module: UUID): Future[Option[JsValue]] =
    db.run(sql"SELECT exams.module_exams(${module.toString}::uuid)".as[Option[JsValue]].head)

  /** Creates the exam and returns it. */
  def create(name: String, modules: Set[UUID]): Future[JsValue] = {
    val id     = UUID.randomUUID()
    val action = assignable(modules) >> (exams += (id, name)) >> (assignments ++= modules.map(id -> _)) >> get(id)
    db.run(action.transactionally)
  }

  /** Replaces name and modules of the exam, writing only what changes, or returns `None` if it does not exist. */
  def update(id: UUID, name: String, modules: Set[UUID]): Future[Option[JsValue]] =
    db.run(
      lock(Set(id)).flatMap { found =>
        if !found then DBIO.successful(None)
        else
          for {
            _       <- assignable(modules)
            _       <- exams.filter(e => e.id === id && e.name =!= name).map(_.name).update(name)
            _       <- assignments.filter(a => a.exam === id && !a.module.inSet(modules)).delete
            current <- assignments.filter(_.exam === id).map(_.module).result
            _       <- assignments ++= (modules -- current).map(id -> _)
            json    <- get(id)
          } yield Some(json)
      }.transactionally
    )

  /**
   * Deletes the exam unless an exam appointment or a published exam list entry references it.
   *
   * @return `None` if the exam does not exist, `Some(false)` if it is referenced
   */
  def delete(id: UUID): Future[Option[Boolean]] =
    db.run(
      lock(Set(id)).flatMap { found =>
        if !found then DBIO.successful(None)
        else
          sql"""SELECT EXISTS (SELECT FROM schedule.exam_entry_draft WHERE exam = ${id.toString}::uuid)
                    OR EXISTS (SELECT FROM schedule.exam_entry WHERE exam = ${id.toString}::uuid)
                    OR EXISTS (SELECT FROM exams.exam_list_entry WHERE exams @> ${Json.arr(Json.obj("exam" -> id))})"""
            .as[Boolean]
            .head
            .flatMap(referenced =>
              if referenced then DBIO.successful(Some(false))
              else exams.filter(_.id === id).delete.map(_ => Some(true))
            )
      }.transactionally
    )

  /**
   * Merges the sources into the target, see `exams.merge_exams`, or returns `None` if an exam does not exist. The
   * modules of the sources are locked after the exams, so that a module sync cannot take their own exams while their
   * assignments are copied to the target.
   */
  def merge(target: UUID, sources: Set[UUID], name: String): Future[Option[JsValue]] =
    db.run(
      lock(sources + target).flatMap { found =>
        if !found then DBIO.successful(None)
        else
          lockModules(_.id.in(assignments.filter(_.exam.inSet(sources)).map(_.module))) >>
            sql"SELECT exams.merge_exams(${target.toString}::uuid, ${sources.toList}::uuid[], $name)"
              .as[Option[String]] >> get(target).map(Some(_))
      }.transactionally
    )

  /**
   * Fails unless all modules exist and have own exams. The rows are locked before the check, so that module syncs,
   * which lock them first as well, cannot change the eligibility until the transaction ends.
   */
  private def assignable(modules: Set[UUID]): DBIO[Unit] =
    for {
      _     <- lockModules(_.id.inSet(modules))
      valid <- TableQuery[ModuleTable].filter(m => m.id.inSet(modules) && m.hasOwnExams).map(_.id).result
      invalid = modules -- valid
      _ <-
        if invalid.isEmpty then DBIO.unit
        else
          DBIO.failed(
            IllegalArgumentException(s"unknown modules or modules without own exams: ${invalid.mkString(", ")}")
          )
    } yield ()

  /** Locks the exams in a fixed order and returns whether all exist. */
  private def lock(ids: Set[UUID]): DBIO[Boolean] =
    exams.filter(_.id.inSet(ids)).sortBy(_.id).map(_.id).forUpdate.result.map(_.size == ids.size)

  /** Locks the selected module rows in a fixed order. */
  private def lockModules(selected: ModuleTable => Rep[Boolean]): DBIO[Seq[UUID]] =
    TableQuery[ModuleTable].filter(selected).sortBy(_.id).map(_.id).forUpdate.result

  private def get(id: UUID): DBIO[JsValue] = sql"SELECT exams.get_exams(${List(id)}::uuid[]) -> 0".as[JsValue].head
}
