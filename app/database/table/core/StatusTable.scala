package database.table.core

import database.table.IDLabelColumn
import database.Schema
import models.core.ModuleStatus
import slick.jdbc.PostgresProfile.api.*

private[database] final class StatusTable(tag: Tag)
    extends Table[ModuleStatus](tag, Some(Schema.Core.name), "status")
    with IDLabelColumn[ModuleStatus] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (ModuleStatus.apply.tupled, ModuleStatus.unapply)
}
