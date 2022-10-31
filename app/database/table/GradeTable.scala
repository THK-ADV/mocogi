package database.table

import basedata.Grade
import slick.jdbc.PostgresProfile.api._

final class GradeTable(tag: Tag)
    extends Table[Grade](tag, "grade")
    with AbbrevLabelDescColumn[Grade] {
  override def * = (
    abbrev,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (Grade.tupled, Grade.unapply)
}
