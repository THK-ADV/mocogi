package models

import java.time.LocalDateTime
import java.util.UUID

import controllers.json.JsonNullWritable
import models.core.IDLabel
import models.core.Identity
import play.api.libs.json.Json
import play.api.libs.json.Writes

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
  type DB     = ModuleReview[String, String]
  type Atomic = ModuleReview[IDLabel, Identity.Person]

  implicit def writesDb: Writes[DB] = Json.writes

  implicit def writesAtomic: Writes[Atomic] = Json.writes
}
