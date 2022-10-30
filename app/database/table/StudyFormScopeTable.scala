package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class StudyFormScopeDbEntry(
    id: UUID,
    studyForm: UUID,
    duration: Int,
    totalEcts: Int,
    deReason: String,
    enReason: String
)

final class StudyFormScopeTable(tag: Tag)
    extends Table[StudyFormScopeDbEntry](tag, "study_form_scope") {

  def id = column[UUID]("id", O.PrimaryKey)

  def studyForm = column[UUID]("study_form")

  def duration = column[Int]("duration")

  def totalEcts = column[Int]("total_ects")

  def deReason = column[String]("de_reason")

  def enReason = column[String]("en_reason")

  def studyFormFk =
    foreignKey("study_form", studyForm, TableQuery[StudyFormTable])(
      _.id
    )

  override def * =
    (
      id,
      studyForm,
      duration,
      totalEcts,
      deReason,
      enReason
    ) <> (StudyFormScopeDbEntry.tupled, StudyFormScopeDbEntry.unapply)
}
