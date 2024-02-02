package database.table

import slick.jdbc.PostgresProfile.api._

case class StudyProgramDbEntry(
    id: String,
    deLabel: String,
    enLabel: String,
    internalAbbreviation: String,
    externalAbbreviation: String,
    degree: String
)

final class StudyProgramTable(tag: Tag)
    extends Table[StudyProgramDbEntry](tag, "study_program")
    with IDLabelColumn[StudyProgramDbEntry] {

  def internalAbbreviation = column[String]("internal_abbreviation")

  def externalAbbreviation = column[String]("external_abbreviation")

  def degree = column[String]("degree")

  def degreeFk =
    foreignKey("degree", degree, TableQuery[DegreeTable])(_.id)

  override def * =
    (
      id,
      deLabel,
      enLabel,
      internalAbbreviation,
      externalAbbreviation,
      degree
    ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          String,
          String,
          String,
          String,
          String,
          String
      )
  ) => StudyProgramDbEntry = {
    case (
          id,
          deLabel,
          enLabel,
          internalAbbreviation,
          externalAbbreviation,
          degree
        ) =>
      StudyProgramDbEntry(
        id,
        deLabel,
        enLabel,
        internalAbbreviation,
        externalAbbreviation,
        degree
      )
  }

  def unmapRow: StudyProgramDbEntry => Option[
    (
        String,
        String,
        String,
        String,
        String,
        String
    )
  ] = { a =>
    Option(
      (
        a.id,
        a.deLabel,
        a.enLabel,
        a.internalAbbreviation,
        a.externalAbbreviation,
        a.degree
      )
    )
  }
}
