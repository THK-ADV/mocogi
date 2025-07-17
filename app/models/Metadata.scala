package models

import java.util.UUID

import cats.data.NonEmptyList
import models.core.*
import models.core.ExamPhases.ExamPhase
import parsing.types.*
import play.api.libs.json.Json
import play.api.libs.json.Writes

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
    examiner: Examiner.Default,
    examPhases: NonEmptyList[ExamPhase],
    workload: ModuleWorkload,
    prerequisites: ModulePrerequisites,
    status: ModuleStatus,
    location: ModuleLocation,
    pos: ModulePOs,
    participants: Option[ModuleParticipants],
    competences: List[ModuleCompetence],
    globalCriteria: List[ModuleGlobalCriteria],
    taughtWith: List[ModuleCore],
    attendanceRequirement: Option[AttendanceRequirement],
    assessmentPrerequisite: Option[AssessmentPrerequisite]
)

object Metadata {
  implicit def writes: Writes[Metadata] = Json.writes
}
