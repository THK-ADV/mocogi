package database.repo

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

  private given GetResult[String] =
    GetResult(_.nextString())

  def getGenericModulesForPO(po: String): Future[String] = {
    val query = sql"select generic_modules_for_po($po::text)".as[String].head
    db.run(query)
  }

  def get(id: UUID): Future[Option[String]] = {
    val query = sql"select get_module_details(${id.toString}::uuid)".as[Option[String]].head
    db.run(query)
  }

  def allModuleCore(): Future[String] = {
    val query = sql"select * from module_core".as[String].head
    db.run(query)
  }

  def allGenericModuleOptions(id: UUID): Future[String] = {
    val query = sql"select get_generic_module_options(${id.toString}::uuid)".as[String].head
    db.run(query)
  }
}
