package parsing.types

import java.util.UUID

import cats.data.NonEmptyList
import models.core.*
import models.core.ExamPhases.ExamPhase
import models.AssessmentPrerequisite
import models.AttendanceRequirement
import models.Examiner
import models.ModuleWorkload

case class ParsedMetadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ParsedModuleRelation],
    credits: Double,
    language: ModuleLanguage,
    duration: Int,
    season: Season,
    responsibilities: ModuleResponsibilities,
    assessmentMethods: ModuleAssessmentMethods,
    examiner: Examiner.Default,
    examPhases: NonEmptyList[ExamPhase],
    workload: ModuleWorkload,
    prerequisites: ParsedPrerequisites,
    status: ModuleStatus,
    location: ModuleLocation,
    pos: ParsedPOs,
    participants: Option[ModuleParticipants],
    taughtWith: List[UUID],
    attendanceRequirement: Option[AttendanceRequirement],
    assessmentPrerequisite: Option[AssessmentPrerequisite]
)
