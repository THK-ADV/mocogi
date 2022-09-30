package validator

import basedata.{Competence, GlobalCriteria, Language, Location, ModuleType, Season, Status}
import parsing.types._

import java.util.UUID

case class ValidMetadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ValidModuleRelation],
    ects: ECTS,
    language: Language,
    duration: Int,
    season: Season,
    responsibilities: Responsibilities,
    assessmentMethods: AssessmentMethods,
    workload: ValidWorkload,
    prerequisites: ValidPrerequisites,
    status: Status,
    location: Location,
    validPOs: ValidPOs,
    participants: Option[Participants],
    competences: List[Competence],
    globalCriteria: List[GlobalCriteria],
    taughtWith: List[Module]
)
