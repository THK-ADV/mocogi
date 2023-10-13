package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleReviewRequestTable(tag: Tag)
    extends Table[(UUID, UUID, Boolean)](
      tag,
      "module_review_request"
    ) {

  def review = column[UUID]("review", O.PrimaryKey)

  def reviewer = column[UUID]("reviewer", O.PrimaryKey)

  def approved = column[Boolean]("approved")

  def reviewFk =
    foreignKey("review", review, TableQuery[ModuleReviewTable])(
      _.moduleDraft
    )

  def reviewerFk =
    foreignKey("reviewer", reviewer, TableQuery[ModuleReviewerTable])(
      _.id
    )

  override def * = (review, reviewer, approved)
}
