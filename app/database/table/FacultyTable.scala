package database.table

import basedata.Faculty
import slick.jdbc.PostgresProfile.api._

final class FacultyTable(tag: Tag)
    extends Table[Faculty](tag, "faculty")
    with AbbrevLabelColumn[Faculty] {
  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Faculty.tupled, Faculty.unapply)
}
