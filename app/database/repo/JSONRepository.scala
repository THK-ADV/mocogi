package database.repo

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.{GetResult, JdbcProfile}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// TODO: Use this class for every function call which is directly exposed as json to the REST API
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
}
