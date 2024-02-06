package database.table.core

import database.table.IDLabelColumn
import models.core.Status
import slick.jdbc.PostgresProfile.api._

final class StatusTable(tag: Tag)
    extends Table[Status](tag, "status")
    with IDLabelColumn[Status] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((Status.apply _).tupled, Status.unapply)
}
