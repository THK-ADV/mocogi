package service.moduledetails

import java.time.LocalDateTime
import java.util.UUID

import controllers.json.JsonNullWritable
import models.AssessmentPrerequisite
import models.AttendanceRequirement
import models.ModuleWorkload
import parsing.types.ModuleParticipants
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.OFormat

final case class ModuleDetails(
    id: UUID,
    lastModified: LocalDateTime,
    title: String,
    abbreviation: String,
    moduleType: JsObject,
    ects: Double,
    language: JsObject,
    duration: Int,
    season: String,
    workload: ModuleWorkload,
    status: JsObject,
    location: String,
    firstExaminer: JsObject,
    secondExaminer: JsObject,
    examPhases: List[String],
    participants: Option[ModuleParticipants],
    recommendedPrerequisites: Option[JsObject],
    requiredPrerequisites: Option[JsObject],
    deContent: JsObject,
    enContent: JsObject,
    moduleManagement: List[JsObject],
    lecturer: List[JsObject],
    assessments: List[JsObject],
    poMandatory: List[JsObject],
    poOptional: List[JsObject],
    taughtWith: List[JsObject],
    moduleRelation: Option[JsObject],
    attendanceRequirement: Option[AttendanceRequirement],
    assessmentPrerequisite: Option[AssessmentPrerequisite]
)

object ModuleDetails extends JsonNullWritable {
  given OFormat[ModuleDetails] = Json.format
}
