package database.table

import models.UserBranch
import slick.jdbc.PostgresProfile.api._

final class UserBranchTable(tag: Tag)
    extends Table[UserBranch](tag, "user_has_branch") {
  def user = column[String]("user", O.PrimaryKey)

  def branch = column[String]("branch_id")

  def commitId = column[Option[String]]("commit_id")

  def mergeRequestId = column[Option[Int]]("merge_request_id")

  override def * =
    (
      user,
      branch,
      commitId,
      mergeRequestId
    ) <> (UserBranch.tupled, UserBranch.unapply)
}
