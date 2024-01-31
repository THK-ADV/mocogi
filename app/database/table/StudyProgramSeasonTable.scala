package database.table

import slick.jdbc.PostgresProfile.api._

case class StudyProgramSeasonDbEntry(
    season: String,
    studyProgram: String
)

final class StudyProgramSeasonTable(tag: Tag)
    extends Table[StudyProgramSeasonDbEntry](tag, "study_program_season") {

  def season = column[String]("season", O.PrimaryKey)

  def studyProgram = column[String]("study_program", O.PrimaryKey)

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.id
    )

  override def * =
    (
      season,
      studyProgram
    ) <> (StudyProgramSeasonDbEntry.tupled, StudyProgramSeasonDbEntry.unapply)
}
