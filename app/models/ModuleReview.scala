package models

import java.util.UUID

case class ModuleReview(
    id: UUID,
    moduleDraft: UUID,
    role: UniversityRole,
    status: ModuleReviewStatus,
    studyProgram: String,
    comment: Option[String]
)
