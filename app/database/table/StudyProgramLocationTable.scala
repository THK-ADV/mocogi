package database.table

import slick.jdbc.PostgresProfile.api._

case class StudyProgramLocationDbEntry(
    location: String,
    studyProgram: String
)

final class StudyProgramLocationTable(tag: Tag)
    extends Table[StudyProgramLocationDbEntry](tag, "study_program_location") {

  def location = column[String]("location", O.PrimaryKey)

  def studyProgram = column[String]("study_program", O.PrimaryKey)

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.id
    )

  override def * =
    (
      location,
      studyProgram
    ) <> (StudyProgramLocationDbEntry.tupled, StudyProgramLocationDbEntry.unapply)
}
