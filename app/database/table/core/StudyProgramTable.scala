package database.table.core

import database.table.IDLabelColumn
import slick.jdbc.PostgresProfile.api._

private[database] case class StudyProgramDbEntry(
    id: String,
    deLabel: String,
    enLabel: String,
    abbreviation: String,
    degree: String
)

private[database] final class StudyProgramTable(tag: Tag)
    extends Table[StudyProgramDbEntry](tag, "study_program")
    with IDLabelColumn[StudyProgramDbEntry] {

  def degree = column[String]("degree")

  def abbreviation = column[String]("abbreviation")

  def degreeFk =
    foreignKey("degree", degree, TableQuery[DegreeTable])(_.id)

  override def * =
    (
      id,
      deLabel,
      enLabel,
      abbreviation,
      degree
    ) <> (StudyProgramDbEntry.apply, StudyProgramDbEntry.unapply)
}
