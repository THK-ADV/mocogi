package models

import controllers.JsonNullWritable
import play.api.libs.json.{Json, Writes}

case class StudyProgramView(
    poId: String,
    poVersion: Int,
    studyProgramId: String,
    studyProgramLabel: String,
    gradeLabel: String,
    specialization: Option[SpecializationShort]
)

object StudyProgramView extends JsonNullWritable {
  implicit def writes: Writes[StudyProgramView] = Json.writes
}
