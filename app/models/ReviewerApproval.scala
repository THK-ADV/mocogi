package models

import models.core.AbbrevLabelLike
import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class ReviewerApproval(
    reviewId: UUID,
    moduleId: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    author: String,
    role: UniversityRole,
    status: ModuleReviewSummaryStatus,
    studyProgram: AbbrevLabelLike,
    canReview: Boolean
)

object ReviewerApproval {
  implicit def writes: Writes[ReviewerApproval] =
    Json.writes[ReviewerApproval]
}
