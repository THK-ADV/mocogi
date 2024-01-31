package database.table

import models.core.Faculty
import slick.jdbc.PostgresProfile.api._

final class FacultyTable(tag: Tag)
    extends Table[Faculty](tag, "faculty")
    with IDLabelColumn[Faculty] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((Faculty.apply _).tupled, Faculty.unapply)
}
