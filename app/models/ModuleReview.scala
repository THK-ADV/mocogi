package models

import java.util.UUID

case class ModuleReview(
    moduleDraft: UUID,
    status: ModuleReviewStatus,
    requests: Seq[ModuleReviewRequest]
)
