package database.table

import basedata.Location
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
