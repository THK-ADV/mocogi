package validator

import models.ModuleCore
import models.core._
import parsing.types._
import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class Metadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ModuleRelation],
    ects: ModuleECTS,
    language: ModuleLanguage,
    duration: Int,
    season: Season,
    responsibilities: ModuleResponsibilities,
    assessmentMethods: ModuleAssessmentMethods,
    workload: ModuleWorkload,
    prerequisites: ModulePrerequisites,
    status: ModuleStatus,
    location: ModuleLocation,
    pos: ModulePOs,
    participants: Option[ModuleParticipants],
    competences: List[ModuleCompetence],
    globalCriteria: List[ModuleGlobalCriteria],
    taughtWith: List[ModuleCore]
)

object Metadata {
  implicit def writes: Writes[Metadata] = Json.writes
}
