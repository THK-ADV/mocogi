package parsing.types

import models.core.{ModuleCompetence, ModuleGlobalCriteria, ModuleLanguage, ModuleLocation, ModuleType, Season, ModuleStatus}

import java.util.UUID

case class ParsedMetadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ParsedModuleRelation],
    credits: Either[Double, List[ModuleECTSFocusAreaContribution]],
    language: ModuleLanguage,
    duration: Int,
    season: Season,
    responsibilities: ModuleResponsibilities,
    assessmentMethods: ModuleAssessmentMethods,
    workload: ParsedWorkload,
    prerequisites: ParsedPrerequisites,
    status: ModuleStatus,
    location: ModuleLocation,
    pos: ParsedPOs,
    participants: Option[ModuleParticipants],
    competences: List[ModuleCompetence],
    globalCriteria: List[ModuleGlobalCriteria],
    taughtWith: List[UUID]
)
