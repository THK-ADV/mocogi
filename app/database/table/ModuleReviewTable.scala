package database.table

import models.{ModuleReview, ModuleReviewStatus, UniversityRole}
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID

final class ModuleReviewTable(tag: Tag)
    extends Table[ModuleReview.DB](
      tag,
      "module_review"
    ) {

  def id = column[UUID]("id", O.PrimaryKey)

  def moduleDraft = column[UUID]("module_draft")

  def role = column[UniversityRole]("role")

  def status = column[ModuleReviewStatus]("status")

  def studyProgram = column[String]("study_program")

  def comment = column[Option[String]]("comment")

  def respondedBy = column[Option[String]]("responded_by")

  def respondedAt = column[Option[LocalDateTime]]("responded_at")

  override def * =
    (
      id,
      moduleDraft,
      role,
      status,
      studyProgram,
      comment,
      respondedBy,
      respondedAt
    ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          UUID,
          UUID,
          UniversityRole,
          ModuleReviewStatus,
          String,
          Option[String],
          Option[String],
          Option[LocalDateTime]
      )
  ) => ModuleReview.DB = {
    case (
          id,
          moduleDraft,
          role,
          status,
          studyProgram,
          comment,
          respondedBy,
          respondedAt
        ) =>
      ModuleReview(
        id,
        moduleDraft,
        role,
        status,
        studyProgram,
        comment,
        respondedBy,
        respondedAt
      )
  }

  def unmapRow(arg: ModuleReview.DB): Option[
    (
        UUID,
        UUID,
        UniversityRole,
        ModuleReviewStatus,
        String,
        Option[String],
        Option[String],
        Option[LocalDateTime]
    )
  ] =
    Some(
      (
        arg.id,
        arg.moduleDraft,
        arg.role,
        arg.status,
        arg.studyProgram,
        arg.comment,
        arg.respondedBy,
        arg.respondedAt
      )
    )
}
