package database.table

import auth.CampusId
import models.ModuleUpdatePermissionType
import models.ModuleUpdatePermissionType.{Granted, Inherited}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleUpdatePermissionTable(tag: Tag)
    extends Table[(UUID, CampusId, ModuleUpdatePermissionType)](
      tag,
      "module_update_permission"
    ) {

  def module = column[UUID]("module", O.PrimaryKey)

  def campusId = column[CampusId]("campus_id", O.PrimaryKey)

  def kind = column[ModuleUpdatePermissionType]("kind")

  def isInherited = {
    val inherited: ModuleUpdatePermissionType = Inherited
    kind === inherited
  }

  def isGranted = {
    val granted: ModuleUpdatePermissionType = Granted
    kind === granted
  }

  def * = (module, campusId, kind)
}
