package database.table

import basedata.Status
import slick.jdbc.PostgresProfile.api._

final class StatusTable(tag: Tag) extends Table[Status](tag, "status") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Status.tupled, Status.unapply)
}
