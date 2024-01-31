package validator

import models.Module
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
    ects: ECTS,
    language: Language,
    duration: Int,
    season: Season,
    responsibilities: Responsibilities,
    assessmentMethods: AssessmentMethods,
    workload: Workload,
    prerequisites: Prerequisites,
    status: Status,
    location: Location,
    validPOs: POs,
    participants: Option[Participants],
    competences: List[Competence],
    globalCriteria: List[GlobalCriteria],
    taughtWith: List[Module]
)

object Metadata {
  implicit def writes: Writes[Metadata] = Json.writes
}
