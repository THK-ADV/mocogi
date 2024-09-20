package database.table.core

import database.table.IDLabelColumn
import models.core.Season
import slick.jdbc.PostgresProfile.api._

final class SeasonTable(tag: Tag) extends Table[Season](tag, "season") with IDLabelColumn[Season] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((Season.apply _).tupled, Season.unapply)
}
