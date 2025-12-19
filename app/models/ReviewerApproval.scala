package models

import java.util.UUID

import models.core.Degree
import models.core.IDLabel
import models.core.Identity
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ReviewerApproval(
    reviewId: UUID,
    moduleId: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    author: Identity.Person,
    role: UniversityRole,
    status: ModuleReviewSummaryStatus,
    studyProgram: IDLabel,
    degree: Degree,
    canReview: Boolean
)

object ReviewerApproval {
  implicit def writes: Writes[ReviewerApproval] = Json.writes
}
