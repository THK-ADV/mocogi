package database.table

import basedata.Season
import slick.jdbc.PostgresProfile.api._

final class SeasonTable(tag: Tag) extends Table[Season](tag, "season") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Season.tupled, Season.unapply)
}
