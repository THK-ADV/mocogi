package models

import java.util.UUID

case class ModuleReviewer(
    id: UUID,
    user: User,
    role: ModuleReviewerRole,
    studyProgram: String
)
