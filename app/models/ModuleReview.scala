package models

import controllers.JsonNullWritable
import models.core.Identity
import play.api.libs.json.{Json, Writes}

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

object ModuleReview extends JsonNullWritable {
  type DB = ModuleReview[String, String]
  type Atomic = ModuleReview[StudyProgramShort, Identity.Person]

  implicit def writesDb: Writes[DB] = Json.writes

  implicit def writesAtomic: Writes[Atomic] = Json.writes
}
