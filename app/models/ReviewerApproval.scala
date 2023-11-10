package models

import controllers.formats.{GradesFormat, PersonFormat}
import models.core.{AbbrevLabelLike, Grade, Person}
import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class ReviewerApproval(
    reviewId: UUID,
    moduleId: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    author: Person.Default,
    role: UniversityRole,
    status: ModuleReviewSummaryStatus,
    studyProgram: AbbrevLabelLike,
    grade: Grade,
    canReview: Boolean
)

object ReviewerApproval extends PersonFormat with GradesFormat {
  implicit def writes: Writes[ReviewerApproval] =
    Json.writes[ReviewerApproval]
}
