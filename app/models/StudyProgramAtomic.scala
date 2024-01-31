package models

import controllers.JsonNullWritable
import play.api.libs.json.{Json, Writes}

case class StudyProgramAtomic(
    poId: String,
    poVersion: Int,
    studyProgramId: String,
    studyProgramLabel: String,
    gradeLabel: String,
    specialization: Option[SpecializationShort]
)

object StudyProgramAtomic extends JsonNullWritable {
  implicit def writes: Writes[StudyProgramAtomic] = Json.writes
}
