package database.table

import models.User
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class UserTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[UUID]("id", O.PrimaryKey)

  def username = column[String]("username")

  override def * = (id, username) <> (User.tupled, User.unapply)
}
