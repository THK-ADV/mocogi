package database.table

import models.{ModuleUpdatePermissionType, User}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleUpdatePermissionTable(tag: Tag)
    extends Table[(UUID, User, ModuleUpdatePermissionType)](
      tag,
      "module_update_permission"
    ) {

  def module = column[UUID]("module_id", O.PrimaryKey)

  def user = column[User]("user_id", O.PrimaryKey)

  def kind = column[String]("kind")

  def moduleFk =
    foreignKey("module", module, TableQuery[ModuleCompendiumTable])(_.id)

  def * = (module, user, kind) <> (mapRow, unmapRow)

  def mapRow
      : ((UUID, User, String)) => (UUID, User, ModuleUpdatePermissionType) = {
    case (a, b, c) => (a, b, ModuleUpdatePermissionType(c))
  }

  def unmapRow: ((UUID, User, ModuleUpdatePermissionType)) => Option[
    (UUID, User, String)
  ] = { case (a, b, c) => Option((a, b, c.value)) }
}
