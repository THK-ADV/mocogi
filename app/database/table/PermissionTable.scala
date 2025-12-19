package database.table

import permission.PermissionType
import slick.jdbc.PostgresProfile.api.*

private[database] case class Permission(
    id: Long,
    permType: PermissionType,
    person: String,
    context: Option[List[String]]
)

private[database] final class PermissionTable(tag: Tag) extends Table[Permission](tag, "permission") {

  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  given BaseColumnType[PermissionType] =
    MappedColumnType.base[PermissionType, String](_.label, PermissionType.apply)

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def permType = column[PermissionType]("type")

  def person = column[String]("person")

  def context = column[Option[List[String]]]("context")

  override def * = (id, permType, person, context) <> (Permission.apply, Permission.unapply)
}
