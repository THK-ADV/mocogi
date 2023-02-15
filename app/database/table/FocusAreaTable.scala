package database.table

import models.core.FocusArea
import slick.jdbc.PostgresProfile.api._

final class FocusAreaTable(tag: Tag)
    extends Table[FocusArea](tag, "focus_area") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  def deDesc = column[String]("de_desc")

  def enDesc = column[String]("en_desc")

  def studyProgram = column[String]("study_program")

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.abbrev
    )

  override def * = (
    abbrev,
    studyProgram,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (FocusArea.tupled, FocusArea.unapply)
}
