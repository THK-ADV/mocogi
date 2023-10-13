package models

import java.util.UUID

case class ModuleReviewRequest(reviewer: UUID, approved: Boolean)
