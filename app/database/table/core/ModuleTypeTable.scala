package database.table.core

import database.table.IDLabelColumn
import database.Schema
import models.core.ModuleType
import slick.jdbc.PostgresProfile.api.*

private[database] final class ModuleTypeTable(tag: Tag)
    extends Table[ModuleType](tag, Some(Schema.Core.name), "module_type")
    with IDLabelColumn[ModuleType] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (ModuleType.apply.tupled, ModuleType.unapply)
}
