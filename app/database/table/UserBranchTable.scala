package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class UserBranch(user: UUID, branch: String)

final class UserBranchTable(tag: Tag)
    extends Table[UserBranch](tag, "user_has_branch") {
  def user = column[UUID]("user", O.PrimaryKey)

  def branch = column[String]("branch_id")

  def userFk =
    foreignKey("user", user, TableQuery[UserTable])(_.id)

  override def * = (user, branch) <> (UserBranch.tupled, UserBranch.unapply)
}
