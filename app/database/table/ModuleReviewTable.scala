package database.table

import models.{ModuleReview, ModuleReviewStatus, UniversityRole}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleReviewTable(tag: Tag)
    extends Table[ModuleReview](
      tag,
      "module_review"
    ) {

  def id = column[UUID]("id", O.PrimaryKey)

  def moduleDraft = column[UUID]("module_draft")

  def role = column[UniversityRole]("role")

  def status = column[ModuleReviewStatus]("status")

  def studyProgram = column[String]("study_program")

  def comment = column[Option[String]]("comment")

  override def * =
    (
      id,
      moduleDraft,
      role,
      status,
      studyProgram,
      comment
    ) <> (ModuleReview.tupled, ModuleReview.unapply)
}
