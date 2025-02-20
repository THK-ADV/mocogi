package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api.*

final class ModuleCompanionTable(tag: Tag) extends Table[(UUID, String)](tag, "module_companion") {
  def module = column[UUID]("module", O.PrimaryKey)
  def po     = column[String]("companion_po", O.PrimaryKey)

  def * = (module, po)
}
