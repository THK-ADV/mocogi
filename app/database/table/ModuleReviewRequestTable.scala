package database.table

import models.{ModuleReviewRequest, UniversityRole}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleReviewRequestTable(tag: Tag)
    extends Table[ModuleReviewRequest](
      tag,
      "module_review_request"
    ) {

  def review = column[UUID]("review", O.PrimaryKey)

  def reviewer = column[String]("reviewer", O.PrimaryKey)

  def status = column[ModuleReviewRequest.Status]("status")

  def reviewFk =
    foreignKey("review", review, TableQuery[ModuleReviewTable])(
      _.moduleDraft
    )

  def reviewerFk =
    foreignKey("reviewer", reviewer, TableQuery[PersonTable])(
      _.id
    )

  override def * = (
    review,
    reviewer,
    status
  ) <> ((ModuleReviewRequest.apply _).tupled, ModuleReviewRequest.unapply)
}
