package database.table

import slick.jdbc.PostgresProfile.api._

case class StudyProgramLanguageDbEntry(
    language: String,
    studyProgram: String
)

final class StudyProgramLanguageTable(tag: Tag)
    extends Table[StudyProgramLanguageDbEntry](tag, "study_program_language") {

  def language = column[String]("language", O.PrimaryKey)

  def studyProgram = column[String]("study_program", O.PrimaryKey)

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.abbrev
    )

  def languageFk =
    foreignKey("language", language, TableQuery[LanguageTable])(_.abbrev)

  override def * =
    (
      language,
      studyProgram
    ) <> (StudyProgramLanguageDbEntry.tupled, StudyProgramLanguageDbEntry.unapply)
}
