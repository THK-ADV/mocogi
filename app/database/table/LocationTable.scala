package database.table

import basedata.Location
import slick.jdbc.PostgresProfile.api._

final class LocationTable(tag: Tag) extends Table[Location](tag, "location") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Location.tupled, Location.unapply)
}
