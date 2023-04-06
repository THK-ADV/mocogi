package database

import parsing.types.{Content, Participants}
import validator.Workload

import java.util.UUID

case class POMandatoryOutput(
    po: String,
    specialization: Option[String],
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)

case class POOptionalOutput(
    po: String,
    specialization: Option[String],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class AssessmentMethodEntryOutput(
    method: String,
    percentage: Option[Double],
    precondition: List[String]
)

case class PrerequisiteEntryOutput(
    text: String,
    modules: List[UUID],
    pos: List[String]
)

case class AssessmentMethodsOutput(
    mandatory: List[AssessmentMethodEntryOutput],
    optional: List[AssessmentMethodEntryOutput]
)

case class PrerequisitesOutput(
    recommended: Option[PrerequisiteEntryOutput],
    required: Option[PrerequisiteEntryOutput]
)

case class POOutput(
    mandatory: List[POMandatoryOutput],
    optional: List[POOptionalOutput]
)

sealed trait ModuleRelationOutput

object ModuleRelationOutput {
  case class Parent(children: List[UUID]) extends ModuleRelationOutput
  case class Child(parent: UUID) extends ModuleRelationOutput
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

case class ModuleCompendiumOutput(
    gitPath: String,
    metadata: MetadataOutput,
    deContent: Content,
    enContent: Content
)
