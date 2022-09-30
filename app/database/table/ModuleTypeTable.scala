package database.table

import basedata.ModuleType
import slick.jdbc.PostgresProfile.api._

final class ModuleTypeTable(tag: Tag)
    extends Table[ModuleType](tag, "module_type") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (ModuleType.tupled, ModuleType.unapply)
}
