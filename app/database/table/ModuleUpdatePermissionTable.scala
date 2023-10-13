package database.table

import models.{ModuleUpdatePermissionType, User}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleUpdatePermissionTable(tag: Tag)
    extends Table[(UUID, User, ModuleUpdatePermissionType)](
      tag,
      "module_update_permission"
    ) {

  def module = column[UUID]("module", O.PrimaryKey)

  def user = column[User]("user_id", O.PrimaryKey)

  def kind = column[ModuleUpdatePermissionType]("kind")

  def moduleFk =
    foreignKey("module", module, TableQuery[ModuleCompendiumTable])(_.id)

  def * = (module, user, kind)
}
