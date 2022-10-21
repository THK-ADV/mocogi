package database.table

import database.entities.FocusAreaDbEntry
import slick.jdbc.PostgresProfile.api._

final class FocusAreaTable(tag: Tag)
    extends Table[FocusAreaDbEntry](tag, "focus_area") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  def deDesc = column[String]("de_desc")

  def enDesc = column[String]("en_desc")

  def program = column[String]("program")

  override def * = (
    abbrev,
    program,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (FocusAreaDbEntry.tupled, FocusAreaDbEntry.unapply)
}
