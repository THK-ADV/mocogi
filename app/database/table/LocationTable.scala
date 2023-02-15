package database.table

import models.core.Location
import slick.jdbc.PostgresProfile.api._

final class LocationTable(tag: Tag)
    extends Table[Location](tag, "location")
    with AbbrevLabelColumn[Location] {
  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Location.tupled, Location.unapply)
}
