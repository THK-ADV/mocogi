package database.table.core

import models.UniversityRole
import slick.jdbc.PostgresProfile.api._

case class StudyProgramPersonDbEntry(
    person: String,
    studyProgram: String,
    role: UniversityRole
)

final class StudyProgramPersonTable(tag: Tag) extends Table[StudyProgramPersonDbEntry](tag, "study_program_person") {

  import database.table.universityRoleColumnType

  def person = column[String]("person", O.PrimaryKey)

  def studyProgram = column[String]("study_program", O.PrimaryKey)

  def role = column[UniversityRole]("role", O.PrimaryKey)

  def isPAV: Rep[Boolean] = role === UniversityRole.PAV

  def personFk =
    foreignKey("person", person, TableQuery[IdentityTable])(_.id)

  def studyProgramFk =
    foreignKey("studyProgram", studyProgram, TableQuery[StudyProgramTable])(
      _.id
    )

  override def * =
    (
      person,
      studyProgram,
      role
    ) <> (StudyProgramPersonDbEntry.apply, StudyProgramPersonDbEntry.unapply)
}
