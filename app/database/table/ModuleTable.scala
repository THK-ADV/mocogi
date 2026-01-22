package database.table

import java.time.LocalDateTime
import java.util.UUID

import cats.data.NonEmptyList
import database.Schema
import models.*
import models.core.ModuleStatus
import models.core.ModuleType
import parsing.types.ModuleContent
import parsing.types.ModuleParticipants
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import slick.jdbc.PostgresProfile.api.*

private[database] case class ModuleDbEntry(
    id: UUID,
    lastModified: LocalDateTime,
    title: String,
    abbrev: String,
    moduleType: String,
    ects: Double,
    language: String,
    duration: Int,
    season: String,
    workload: ModuleWorkload,
    status: String,
    location: String,
    examiner: Examiner.ID,
    examPhases: NonEmptyList[String],
    participants: Option[ModuleParticipants],
    recommendedPrerequisites: Option[ModulePrerequisiteEntryProtocol],
    requiredPrerequisites: Option[ModulePrerequisiteEntryProtocol],
    attendanceRequirement: Option[AttendanceRequirement],
    assessmentPrerequisite: Option[AssessmentPrerequisite],
    deContent: ModuleContent,
    enContent: ModuleContent
)

private[database] final class ModuleTable(tag: Tag)
    extends Table[ModuleDbEntry](tag, Some(Schema.Modules.name), "module") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  given Format[ModuleWorkload] = Json.format[ModuleWorkload]

  def id = column[UUID]("id", O.PrimaryKey)

  def lastModified = column[LocalDateTime]("last_modified")

  def title = column[String]("title")

  def abbrev = column[String]("abbrev")

  def moduleType = column[String]("module_type")

  def ects = column[Double]("ects")

  def language = column[String]("language")

  def duration = column[Int]("duration")

  def season = column[String]("season")

  def workload = column[JsValue]("workload")

  def status = column[String]("status")

  def location = column[String]("location")

  def firstExaminer = column[String]("first_examiner")

  def secondExaminer = column[String]("second_examiner")

  def examPhases = column[List[String]]("exam_phases")

  def participants = column[Option[JsValue]]("participants")

  def recommendedPrerequisites = column[Option[JsValue]]("recommended_prerequisites")

  def requiredPrerequisites = column[Option[JsValue]]("required_prerequisites")

  def attendanceRequirement = column[Option[JsValue]]("attendance_requirement")

  def assessmentPrerequisite = column[Option[JsValue]]("assessment_prerequisite")

  def learningOutcomeDe = column[String]("learning_outcome_de")

  def learningOutcomeEn = column[String]("learning_outcome_en")

  def moduleContentDe = column[String]("module_content_de")

  def moduleContentEn = column[String]("module_content_en")

  def learningMethodsDe = column[String]("learning_methods_de")

  def learningMethodsEn = column[String]("learning_methods_en")

  def literatureDe = column[String]("literature_de")

  def literatureEn = column[String]("literature_en")

  def particularitiesDe = column[String]("particularities_de")

  def particularitiesEn = column[String]("particularities_en")

  def isActive(): Rep[Boolean] =
    this.status === ModuleStatus.activeId

  def isGeneric: Rep[Boolean] =
    this.moduleType === ModuleType.genericId

  override def * = (
    id,
    lastModified,
    title,
    abbrev,
    moduleType,
    ects,
    language,
    duration,
    season,
    workload,
    status,
    location,
    (firstExaminer, secondExaminer),
    examPhases,
    participants,
    (recommendedPrerequisites, requiredPrerequisites),
    (attendanceRequirement, assessmentPrerequisite),
    (learningOutcomeDe, learningOutcomeEn),
    (moduleContentDe, moduleContentEn),
    (learningMethodsDe, learningMethodsEn),
    (literatureDe, literatureEn),
    (particularitiesDe, particularitiesEn),
  ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          UUID,
          LocalDateTime,
          String,
          String,
          String,
          Double,
          String,
          Int,
          String,
          JsValue,
          String,
          String,
          (String, String),
          List[String],
          Option[JsValue],
          (Option[JsValue], Option[JsValue]),
          (Option[JsValue], Option[JsValue]),
          (String, String),
          (String, String),
          (String, String),
          (String, String),
          (String, String)
      )
  ) => ModuleDbEntry = {
    case (
          id,
          lastModified,
          title,
          abbrev,
          moduleType,
          ects,
          language,
          duration,
          season,
          workload,
          status,
          location,
          (firstExaminer, secondExaminer),
          examPhases,
          participants,
          (recommendedPrerequisites, requiredPrerequisites),
          (attendanceRequirement, assessmentPrerequisite),
          (learningOutcomeDe, learningOutcomeEn),
          (moduleContentDe, moduleContentEn),
          (learningMethodsDe, learningMethodsEn),
          (literatureDe, literatureEn),
          (particularitiesDe, particularitiesEn),
        ) =>
      ModuleDbEntry(
        id,
        lastModified,
        title,
        abbrev,
        moduleType,
        ects,
        language,
        duration,
        season,
        Json.fromJson(workload)(given_Format_ModuleWorkload).get,
        status,
        location,
        Examiner(firstExaminer, secondExaminer),
        NonEmptyList.fromListUnsafe(examPhases),
        participants.map(j => Json.fromJson[ModuleParticipants](j).get),
        recommendedPrerequisites.map(j => Json.fromJson[ModulePrerequisiteEntryProtocol](j).get),
        requiredPrerequisites.map(j => Json.fromJson[ModulePrerequisiteEntryProtocol](j).get),
        attendanceRequirement.map(j => Json.fromJson[AttendanceRequirement](j).get),
        assessmentPrerequisite.map(j => Json.fromJson[AssessmentPrerequisite](j).get),
        ModuleContent(
          learningOutcomeDe,
          moduleContentDe,
          learningMethodsDe,
          literatureDe,
          particularitiesDe
        ),
        ModuleContent(
          learningOutcomeEn,
          moduleContentEn,
          learningMethodsEn,
          literatureEn,
          particularitiesEn
        )
      )
  }

  def unmapRow: ModuleDbEntry => Option[
    (
        UUID,
        LocalDateTime,
        String,
        String,
        String,
        Double,
        String,
        Int,
        String,
        JsValue,
        String,
        String,
        (String, String),
        List[String],
        Option[JsValue],
        (Option[JsValue], Option[JsValue]),
        (Option[JsValue], Option[JsValue]),
        (String, String),
        (String, String),
        (String, String),
        (String, String),
        (String, String)
    )
  ] =
    a =>
      Option(
        a.id,
        a.lastModified,
        a.title,
        a.abbrev,
        a.moduleType,
        a.ects,
        a.language,
        a.duration,
        a.season,
        Json.toJson(a.workload)(given_Format_ModuleWorkload),
        a.status,
        a.location,
        (a.examiner.first, a.examiner.second),
        a.examPhases.toList,
        a.participants.map(Json.toJson),
        (a.recommendedPrerequisites.map(Json.toJson), a.requiredPrerequisites.map(Json.toJson)),
        (a.attendanceRequirement.map(Json.toJson), a.assessmentPrerequisite.map(Json.toJson)),
        (a.deContent.learningOutcome, a.enContent.learningOutcome),
        (a.deContent.content, a.enContent.content),
        (a.deContent.teachingAndLearningMethods, a.enContent.teachingAndLearningMethods),
        (a.deContent.recommendedReading, a.enContent.recommendedReading),
        (a.deContent.particularities, a.enContent.particularities)
      )
}
