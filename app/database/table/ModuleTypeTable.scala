package database.table

import models.core.ModuleType
import slick.jdbc.PostgresProfile.api._

final class ModuleTypeTable(tag: Tag)
    extends Table[ModuleType](tag, "module_type")
    with AbbrevLabelColumn[ModuleType] {
  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (ModuleType.tupled, ModuleType.unapply)
}
