package controllers.json

import java.util.UUID

import cats.data.NonEmptyList
import models.*
import models.core.ExamPhases.ExamPhase
import parsing.types.ModuleContent
import parsing.types.ModuleParticipants
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads

/**
 * This class must be a subset of ModuleProtocol. This class is used instead of
 * ModuleProtocol for JSON input to support optional fields which will be set
 * to default values when converted to ModuleProtocol
 */
case class ModuleJson(
    id: Option[UUID],
    metadata: MetadataJson,
    deContent: ModuleContent,
    enContent: ModuleContent
) {
  def toProtocol =
    ModuleProtocol(
      this.id,
      MetadataProtocol(
        this.metadata.title,
        this.metadata.abbrev,
        this.metadata.moduleType,
        this.metadata.ects,
        this.metadata.language,
        this.metadata.duration,
        this.metadata.season,
        this.metadata.workload,
        this.metadata.status,
        this.metadata.location,
        this.metadata.participants,
        this.metadata.moduleRelation,
        this.metadata.moduleManagement,
        this.metadata.lecturers,
        this.metadata.assessmentMethods,
        this.metadata.examiner.getOrElse(Examiner.NN),
        this.metadata.examPhases
          .flatMap(NonEmptyList.fromList)
          .getOrElse(NonEmptyList.one(ExamPhase.none.id)),
        this.metadata.prerequisites,
        this.metadata.po,
        this.metadata.taughtWith,
        this.metadata.attendanceRequirement,
        this.metadata.assessmentPrerequisite
      ),
      this.deContent,
      this.enContent
    )
}

case class MetadataJson(
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
    examiner: Option[Examiner.ID],
    examPhases: Option[List[String]],
    prerequisites: ModulePrerequisitesProtocol,
    po: ModulePOProtocol,
    taughtWith: List[UUID],
    attendanceRequirement: Option[AttendanceRequirement],
    assessmentPrerequisite: Option[AssessmentPrerequisite]
)

object ModuleJson {
  private val defaultReads: Reads[ModuleJson] = Json.reads

  implicit def reads: Reads[ModuleJson] = Reads { json =>
    defaultReads.reads(ignoreLegacyChildRelation(json))
  }

  private def ignoreLegacyChildRelation(json: JsValue): JsValue =
    json match {
      case module: JsObject =>
        (module \ "metadata").asOpt[JsObject] match {
          case Some(metadata) if (metadata \ "moduleRelation" \ "kind").asOpt[String].contains("child") =>
            module + ("metadata" -> (metadata + ("moduleRelation" -> JsNull)))
          case _ => module
        }
      case other => other
    }
}

object MetadataJson extends JsonNullWritable with NelWrites {
  implicit def reads: Reads[MetadataJson] = Json.reads
}
