package models

import controllers.JsonNullWritable
import parsing.types.ModuleParticipants
import play.api.libs.json.{Format, Json}
import validator.ModuleWorkload

import java.util.UUID

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
    moduleManagement: List[String],
    lecturers: List[String],
    assessmentMethods: ModuleAssessmentMethodsProtocol,
    prerequisites: ModulePrerequisitesProtocol,
    po: ModulePOProtocol,
    competences: List[String],
    globalCriteria: List[String],
    taughtWith: List[UUID]
)

object MetadataProtocol extends JsonNullWritable {
  implicit def format: Format[MetadataProtocol] = Json.format
}
