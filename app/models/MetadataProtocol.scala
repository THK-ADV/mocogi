package models

import cats.data.NonEmptyList
import controllers.{JsonNullWritable, NelWrites}
import parsing.types.ModuleParticipants
import play.api.libs.json.{Format, Json}

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
    moduleManagement: NonEmptyList[String],
    lecturers: NonEmptyList[String],
    assessmentMethods: ModuleAssessmentMethodsProtocol,
    examiner: Examiner.ID,
    examPhases: NonEmptyList[String],
    prerequisites: ModulePrerequisitesProtocol,
    po: ModulePOProtocol,
    competences: List[String],
    globalCriteria: List[String],
    taughtWith: List[UUID]
)

object MetadataProtocol extends JsonNullWritable with NelWrites {
  implicit def format: Format[MetadataProtocol] = Json.format
}
