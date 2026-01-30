package database.table.core

import database.table.IDLabelDescColumn
import database.Schema
import models.core.Degree
import slick.jdbc.PostgresProfile.api.*

private[database] final class DegreeTable(tag: Tag)
    extends Table[Degree](tag, Some(Schema.Core.name), "degree")
    with IDLabelDescColumn[Degree] {
  override def * = (
    id,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (Degree.apply.tupled, Degree.unapply)
}
