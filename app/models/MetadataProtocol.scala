package models

import java.util.UUID

import cats.data.NonEmptyList
import controllers.JsonNullWritable
import controllers.NelWrites
import parsing.types.ModuleParticipants
import play.api.libs.json.Json
import play.api.libs.json.OFormat

// update ModuleProtocolDiff.fields, nonEmptyKeys, diff when new attributes are added
case class MetadataProtocol(
    title: String,
    abbrev: String,
    moduleType: String,
    ects: Double,
    language: String,
    duration: Int,
    season: String,
    workload: ModuleWorkload,
    status: String,
    location: String,
    participants: Option[ModuleParticipants],
    moduleRelation: Option[ModuleRelationProtocol],
    moduleManagement: NonEmptyList[String],
    lecturers: NonEmptyList[String],
    assessmentMethods: ModuleAssessmentMethodsProtocol,
    examiner: Examiner.ID,
    examPhases: NonEmptyList[String],
    prerequisites: ModulePrerequisitesProtocol,
    po: ModulePOProtocol,
    @Deprecated(forRemoval = true) competences: List[String],
    @Deprecated(forRemoval = true) globalCriteria: List[String],
    taughtWith: List[UUID],
    attendanceRequirement: Option[AttendanceRequirement],
    assessmentPrerequisite: Option[AssessmentPrerequisite]
)

object MetadataProtocol extends JsonNullWritable with NelWrites {
  implicit def format: OFormat[MetadataProtocol] = Json.format
}
