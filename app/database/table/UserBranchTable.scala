package database.table

import models.UserBranch
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class UserBranchTable(tag: Tag)
    extends Table[UserBranch](tag, "user_has_branch") {
  def user = column[UUID]("user", O.PrimaryKey)

  def branch = column[String]("branch_id")

  def commitId = column[Option[String]]("commit_id")

  def mergeRequestId = column[Option[Int]]("merge_request_id")

  def userFk =
    foreignKey("user", user, TableQuery[UserTable])(_.id)

  override def * =
    (
      user,
      branch,
      commitId,
      mergeRequestId
    ) <> (UserBranch.tupled, UserBranch.unapply)
}
