package database.table

import basedata.Season
import slick.jdbc.PostgresProfile.api._

final class SeasonTable(tag: Tag)
    extends Table[Season](tag, "season")
    with AbbrevLabelColumn[Season] {
  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Season.tupled, Season.unapply)
}
