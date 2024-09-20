package parsing.types

import java.util.UUID

import cats.data.NonEmptyList
import models.core.ExamPhases.ExamPhase
import models.core.ModuleCompetence
import models.core.ModuleGlobalCriteria
import models.core.ModuleLanguage
import models.core.ModuleLocation
import models.core.ModuleStatus
import models.core.ModuleType
import models.core.Season
import models.Examiner

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
