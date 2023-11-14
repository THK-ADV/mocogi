package models

import models.core.{AbbrevLabelLike, Person}

import java.time.LocalDateTime
import java.util.UUID

case class ModuleReview[StudyProgram, Person](
    id: UUID,
    moduleDraft: UUID,
    role: UniversityRole,
    status: ModuleReviewStatus,
    studyProgram: StudyProgram,
    comment: Option[String],
    respondedBy: Option[Person],
    respondedAt: Option[LocalDateTime]
)

object ModuleReview {
  type DB = ModuleReview[String, String]
  type Atomic = ModuleReview[AbbrevLabelLike, Person.Default]
}
