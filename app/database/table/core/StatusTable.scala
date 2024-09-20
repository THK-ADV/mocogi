package database.table.core

import database.table.IDLabelColumn
import models.core.ModuleStatus
import slick.jdbc.PostgresProfile.api._

final class StatusTable(tag: Tag) extends Table[ModuleStatus](tag, "status") with IDLabelColumn[ModuleStatus] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((ModuleStatus.apply _).tupled, ModuleStatus.unapply)
}
