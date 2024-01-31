package models

import models.core.{Grade, IDLabel, Identity}
import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class ReviewerApproval(
    reviewId: UUID,
    moduleId: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    author: Identity.Person,
    role: UniversityRole,
    status: ModuleReviewSummaryStatus,
    studyProgram: IDLabel,
    grade: Grade,
    canReview: Boolean
)

object ReviewerApproval {
  implicit def writes: Writes[ReviewerApproval] = Json.writes
}
