package parsing.types

import cats.data.NonEmptyList
import models.Examiner
import models.core.ExamPhases.ExamPhase
import models.core.{ModuleCompetence, ModuleGlobalCriteria, ModuleLanguage, ModuleLocation, ModuleStatus, ModuleType, Season}

import java.util.UUID

case class ParsedMetadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ParsedModuleRelation],
    credits: Either[Double, NonEmptyList[ModuleECTSFocusAreaContribution]],
    language: ModuleLanguage,
    duration: Int,
    season: Season,
    responsibilities: ModuleResponsibilities,
    assessmentMethods: ModuleAssessmentMethods,
    examiner: Examiner.Default,
    examPhases: NonEmptyList[ExamPhase],
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
