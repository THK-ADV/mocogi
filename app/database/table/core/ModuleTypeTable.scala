package database.table.core

import database.table.IDLabelColumn
import models.core.ModuleType
import slick.jdbc.PostgresProfile.api._

private[database] final class ModuleTypeTable(tag: Tag)
    extends Table[ModuleType](tag, "module_type")
    with IDLabelColumn[ModuleType] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (ModuleType.apply.tupled, ModuleType.unapply)
}
