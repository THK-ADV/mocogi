package database.table

import models.core.Status
import slick.jdbc.PostgresProfile.api._

final class StatusTable(tag: Tag)
    extends Table[Status](tag, "status")
    with AbbrevLabelColumn[Status] {
  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Status.tupled, Status.unapply)
}
