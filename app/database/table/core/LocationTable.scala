package database.table.core

import database.table.IDLabelColumn
import models.core.Location
import slick.jdbc.PostgresProfile.api._

final class LocationTable(tag: Tag)
    extends Table[Location](tag, "location")
    with IDLabelColumn[Location] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((Location.apply _).tupled, Location.unapply)
}
