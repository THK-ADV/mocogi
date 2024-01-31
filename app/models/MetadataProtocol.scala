package models

import controllers.JsonNullWritable
import database.{
  AssessmentMethodsOutput,
  ModuleRelationOutput,
  POOutput,
  PrerequisitesOutput
}
import parsing.types.{ParsedWorkload, Participants}
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
    workload: ParsedWorkload,
    status: String,
    location: String,
    participants: Option[Participants],
    moduleRelation: Option[ModuleRelationOutput],
    moduleManagement: List[String],
    lecturers: List[String],
    assessmentMethods: AssessmentMethodsOutput,
    prerequisites: PrerequisitesOutput,
    po: POOutput,
    competences: List[String],
    globalCriteria: List[String],
    taughtWith: List[UUID]
)

object MetadataProtocol extends JsonNullWritable {
  implicit def format: Format[MetadataProtocol] = Json.format
}
