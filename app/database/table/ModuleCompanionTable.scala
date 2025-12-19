package database.table

import java.util.UUID

import models.ModuleCompanion
import slick.jdbc.PostgresProfile.api.*

private[database] final class ModuleCompanionTable(tag: Tag) extends Table[ModuleCompanion](tag, "module_companion") {
  def module = column[UUID]("module", O.PrimaryKey)
  def po     = column[String]("companion_po", O.PrimaryKey)

  def * = (module, po) <> (ModuleCompanion.apply, ModuleCompanion.unapply)
}
