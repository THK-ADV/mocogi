package database.table.core

import database.table.IDLabelColumn
import database.Schema
import models.core.Season
import slick.jdbc.PostgresProfile.api.*

private[database] final class SeasonTable(tag: Tag)
    extends Table[Season](tag, Some(Schema.Core.name), "season")
    with IDLabelColumn[Season] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (Season.apply.tupled, Season.unapply)
}
