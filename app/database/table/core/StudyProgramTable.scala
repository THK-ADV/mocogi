package database.table.core

import database.table.IDLabelColumn
import slick.jdbc.PostgresProfile.api._

case class StudyProgramDbEntry(
    id: String,
    deLabel: String,
    enLabel: String,
    degree: String
)

final class StudyProgramTable(tag: Tag)
    extends Table[StudyProgramDbEntry](tag, "study_program")
    with IDLabelColumn[StudyProgramDbEntry] {

  def degree = column[String]("degree")

  def degreeFk =
    foreignKey("degree", degree, TableQuery[DegreeTable])(_.id)

  override def * =
    (
      id,
      deLabel,
      enLabel,
      degree
    ) <> (StudyProgramDbEntry.tupled, StudyProgramDbEntry.unapply)
}
