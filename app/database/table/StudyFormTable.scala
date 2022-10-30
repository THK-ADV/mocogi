package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class StudyFormDbEntry(
    id: UUID,
    studyProgram: String,
    kind: String,
    workloadPerEcts: Int
)

final class StudyFormTable(tag: Tag)
    extends Table[StudyFormDbEntry](tag, "study_form") {

  def id = column[UUID]("id", O.PrimaryKey)

  def studyProgram = column[String]("study_program")

  def studyFormType = column[String]("study_form_type")

  def workloadPerEcts = column[Int]("workload_per_ects")

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.abbrev
    )

  def studyFormTypeFk =
    foreignKey(
      "study_form_type",
      studyFormType,
      TableQuery[StudyFormTypeTable]
    )(
      _.abbrev
    )

  override def * =
    (
      id,
      studyProgram,
      studyFormType,
      workloadPerEcts
    ) <> (StudyFormDbEntry.tupled, StudyFormDbEntry.unapply)
}
