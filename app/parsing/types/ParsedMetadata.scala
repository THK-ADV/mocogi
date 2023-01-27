package parsing.types

import basedata.{Competence, GlobalCriteria, Language, Location, ModuleType, Season, Status}

import java.util.UUID

case class ParsedMetadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ParsedModuleRelation],
    credits: Either[Double, List[ECTSFocusAreaContribution]],
    language: Language,
    duration: Int,
    season: Season,
    responsibilities: Responsibilities,
    assessmentMethods: AssessmentMethods,
    workload: ParsedWorkload,
    prerequisites: ParsedPrerequisites,
    status: Status,
    location: Location,
    pos: ParsedPOs,
    participants: Option[Participants],
    competences: List[Competence],
    globalCriteria: List[GlobalCriteria],
    taughtWith: List[UUID]
)
