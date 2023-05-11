package database.repo

import database.table.UserBranchTable
import models.UserBranch
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

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

  def branchForUser(user: String): Future[Option[UserBranch]] =
    retrieve(tableQuery.filter(_.user.toLowerCase === user.toLowerCase()))
      .map(_.headOption)

  def existsByUser(user: String): Future[Boolean] =
    db.run(
      tableQuery.filter(_.user.toLowerCase === user.toLowerCase).exists.result
    )

  def existsByBranch(branch: String): Future[Boolean] =
    db.run(tableQuery.filter(_.branch === branch).exists.result)

  def delete(user: String): Future[Int] =
    db.run(tableQuery.filter(_.user.toLowerCase === user.toLowerCase).delete)

  def hasCommitAndMergeRequest(branch: String) =
    db.run(
      tableQuery
        .filter(a =>
          a.branch === branch && a.commitId.isDefined && a.mergeRequestId.isDefined
        )
        .map(a => (a.commitId.get, a.mergeRequestId.get))
        .result
        .map(_.headOption)
    )

  def allWithOpenedMergeRequests() =
    db.run(
      tableQuery
        .filter(a => a.commitId.isDefined && a.mergeRequestId.isDefined)
        .result
    )

  def updateCommitId(branch: String, commitId: Option[String]) =
    db.run(
      tableQuery.filter(_.branch === branch).map(_.commitId).update(commitId)
    )

  def updateMergeRequestId(branch: String, mergeRequestId: Option[Int]) =
    db.run(
      tableQuery
        .filter(_.branch === branch)
        .map(_.mergeRequestId)
        .update(mergeRequestId)
    )
}
