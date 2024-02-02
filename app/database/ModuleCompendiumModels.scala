package database

import controllers.JsonNullWritable
import parsing.types.{Content, Participants}
import play.api.libs.json._
import validator.Workload

import java.util.UUID

case class POMandatoryOutput(
    po: String,
    specialization: Option[String],
    recommendedSemester: List[Int]
)

object POMandatoryOutput extends JsonNullWritable {
  implicit def format: Format[POMandatoryOutput] = Json.format
}

case class POOptionalOutput(
    po: String,
    specialization: Option[String],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

object POOptionalOutput extends JsonNullWritable {
  implicit def format: Format[POOptionalOutput] = Json.format
}

case class AssessmentMethodEntryOutput(
    method: String,
    percentage: Option[Double],
    precondition: List[String]
)

object AssessmentMethodEntryOutput extends JsonNullWritable {
  implicit val format: Format[AssessmentMethodEntryOutput] = Json.format
}

case class PrerequisiteEntryOutput(
    text: String,
    modules: List[UUID],
    pos: List[String]
)

object PrerequisiteEntryOutput {
  implicit def format: Format[PrerequisiteEntryOutput] = Json.format
}

case class AssessmentMethodsOutput(
    mandatory: List[AssessmentMethodEntryOutput],
    optional: List[AssessmentMethodEntryOutput]
)

object AssessmentMethodsOutput {
  implicit val format: Format[AssessmentMethodsOutput] = Json.format
}

case class PrerequisitesOutput(
    recommended: Option[PrerequisiteEntryOutput],
    required: Option[PrerequisiteEntryOutput]
)

object PrerequisitesOutput extends JsonNullWritable {
  implicit def format: Format[PrerequisitesOutput] = Json.format
}

case class POOutput(
    mandatory: List[POMandatoryOutput],
    optional: List[POOptionalOutput]
)

object POOutput {
  implicit def format: Format[POOutput] = Json.format
}

sealed trait ModuleRelationOutput

object ModuleRelationOutput {
  case class Parent(children: List[UUID]) extends ModuleRelationOutput
  case class Child(parent: UUID) extends ModuleRelationOutput

  implicit def format: Format[ModuleRelationOutput] =
    OFormat.apply(
      js =>
        js.\("kind")
          .validate[String]
          .flatMap {
            case "parent" =>
              js.\("children")
                .validate[List[UUID]]
                .map(ModuleRelationOutput.Parent.apply)
            case "child" =>
              js.\("parent")
                .validate[UUID]
                .map(ModuleRelationOutput.Child.apply)
            case other =>
              JsError(s"expected kind to be parent or child, but was $other")
          },
      {
        case ModuleRelationOutput.Parent(children) =>
          Json.obj(
            "kind" -> "parent",
            "children" -> Json.toJson(children)
          )
        case ModuleRelationOutput.Child(parent) =>
          Json.obj(
            "kind" -> "child",
            "parent" -> Json.toJson(parent)
          )
      }
    )
}

case class MetadataOutput(
    id: UUID,
    title: String,
    abbrev: String,
    moduleType: String,
    ects: Double,
    language: String,
    duration: Int,
    season: String,
    workload: Workload,
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

object MetadataOutput extends JsonNullWritable {
  implicit def writes: Writes[MetadataOutput] = Json.writes
}

case class ModuleCompendiumOutput(
    gitPath: String,
    metadata: MetadataOutput,
    deContent: Content,
    enContent: Content
)

object ModuleCompendiumOutput {
  implicit def writes: Writes[ModuleCompendiumOutput] = Json.writes
}
