package database.table.core

import database.table.IDLabelDescColumn
import models.core.Degree
import slick.jdbc.PostgresProfile.api._

final class DegreeTable(tag: Tag)
    extends Table[Degree](tag, "degree")
    with IDLabelDescColumn[Degree] {
  override def * = (
    id,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> ((Degree.apply _).tupled, Degree.unapply)
}
