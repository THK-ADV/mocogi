package parsing.types

import basedata.{Competence, GlobalCriteria, Language, Location, ModuleType, Season, Status}

import java.util.UUID

case class Metadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ModuleRelation],
    credits: Either[Double, List[ECTSFocusAreaContribution]],
    language: Language,
    duration: Int,
    season: Season,
    responsibilities: Responsibilities,
    assessmentMethods: AssessmentMethods,
    workload: Workload,
    prerequisites: Prerequisites,
    status: Status,
    location: Location,
    pos: POs,
    participants: Option[Participants],
    competences: List[Competence],
    globalCriteria: List[GlobalCriteria],
    taughtWith: List[String]
)
