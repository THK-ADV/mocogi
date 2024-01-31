package database.table

import models.core.Grade
import slick.jdbc.PostgresProfile.api._

final class GradeTable(tag: Tag)
    extends Table[Grade](tag, "grade")
    with IDLabelDescColumn[Grade] {
  override def * = (
    id,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> ((Grade.apply _).tupled, Grade.unapply)
}
