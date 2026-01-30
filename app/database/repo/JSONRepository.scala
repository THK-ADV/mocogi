package database.repo

import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.GetResult
import slick.jdbc.JdbcProfile

@Singleton
final class JSONRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  def getGenericModulesForPO(po: String): Future[String] = {
    val query = sql"select modules.generic_modules_for_po($po::text)".as[String].head
    db.run(query)
  }

  def get(id: UUID): Future[Option[String]] = {
    val query = sql"select modules.get_module_details(${id.toString}::uuid)".as[Option[String]].head
    db.run(query)
  }

  def allModuleCore(): Future[String] = {
    val query = sql"select * from modules.module_core".as[String].head
    db.run(query)
  }

  def allGenericModuleOptions(id: UUID): Future[String] = {
    val query = sql"select modules.get_generic_module_options(${id.toString}::uuid)".as[String].head
    db.run(query)
  }

  def allByNow(now: LocalDate = LocalDate.now): Future[String] = {
    val month = now.getMonthValue
    val year  = now.getYear
    val query = sql"select schedule.semester_plan_by_now($month, $year)".as[String].head
    db.run(query)
  }

  def allTeachingUnits(): Future[String] = {
    val query =
      sql"select coalesce(jsonb_agg(jsonb_build_object('id', tu.id, 'label', tu.label)), '[]'::jsonb) from core.teaching_unit tu"
        .as[String]
        .head
    db.run(query)
  }

  def allRooms(): Future[String] = {
    val query =
      sql"select coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'label', r.label, 'abbrev', r.abbrev) order by r.abbrev), '[]'::jsonb) from schedule.room r"
        .as[String]
        .head
    db.run(query)
  }
}
