package database.table

import models.UniversityRole
import slick.jdbc.PostgresProfile.api._

case class StudyProgramPersonDbEntry(
    person: String,
    studyProgram: String,
    role: UniversityRole
)

final class StudyProgramPersonTable(tag: Tag)
    extends Table[StudyProgramPersonDbEntry](tag, "study_program_person") {

  def person = column[String]("person", O.PrimaryKey)

  def studyProgram = column[String]("study_program", O.PrimaryKey)

  def role = column[UniversityRole]("role", O.PrimaryKey)

  override def * =
    (
      person,
      studyProgram,
      role
    ) <> (StudyProgramPersonDbEntry.tupled, StudyProgramPersonDbEntry.unapply)
}