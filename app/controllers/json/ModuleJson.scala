package controllers.json

import cats.data.NonEmptyList
import controllers.{JsonNullWritable, NelWrites}
import models._
import models.core.ExamPhase
import parsing.types.{ModuleContent, ModuleParticipants}
import play.api.libs.json.{Json, Reads}

import java.util.UUID

/** This class must be a subset of ModuleProtocol. This class is used instead of
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
        this.metadata.competences,
        this.metadata.globalCriteria,
        this.metadata.taughtWith
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
    competences: List[String],
    globalCriteria: List[String],
    taughtWith: List[UUID]
)

object ModuleJson {
  implicit def reads: Reads[ModuleJson] = Json.reads
}

object MetadataJson extends JsonNullWritable with NelWrites {
  implicit def reads: Reads[MetadataJson] = Json.reads
}
