package database.table.core

import database.table.IDLabelColumn
import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] case class StudyProgramDbEntry(
    id: String,
    deLabel: String,
    enLabel: String,
    abbreviation: String,
    degree: String
)

private[database] final class StudyProgramTable(tag: Tag)
    extends Table[StudyProgramDbEntry](tag, Some(Schema.Core.name), "study_program")
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
