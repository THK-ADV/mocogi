package database.repo

import database.table.{UserBranch, UserBranchTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class UserBranchRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[UserBranch, UserBranch, UserBranchTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[UserBranchTable]

  override protected def retrieve(
      query: Query[UserBranchTable, UserBranch, Seq]
  ) =
    db.run(query.result)

  def branchForUser(username: String): Future[Option[UserBranch]] =
    retrieve(
      tableQuery
        .filter(_.userFk.filter(_.username === username).exists)
    ).map(_.headOption)

  def exists(user: UUID): Future[Boolean] =
    db.run(tableQuery.filter(_.user === user).exists.result)

  def delete(user: UUID): Future[Int] =
    db.run(tableQuery.filter(_.user === user).delete)
}
