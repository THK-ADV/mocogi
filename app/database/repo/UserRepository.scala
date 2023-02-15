package database.repo

import database.Filterable
import database.table.UserTable
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class UserRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[User, User, UserTable]
    with HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[User, UserTable] {
  import profile.api._

  protected val tableQuery = TableQuery[UserTable]

  override protected def retrieve(query: Query[UserTable, User, Seq]) =
    db.run(query.result)

  override protected val makeFilter
      : PartialFunction[(String, String), UserTable => Rep[Boolean]] = {
    case ("username", value) => _.username.toLowerCase === value.toLowerCase
  }

  def byUsername(username: String): Future[User] =
    retrieve(allWithFilter(Map(("username", Seq(username))))).single(ctx)
}
