package database.table

import parsing.types.Content
import slick.jdbc.PostgresProfile.api._
import validator.Workload

import java.time.LocalDateTime
import java.util.UUID

case class ModuleCompendiumDbEntry(
    id: UUID,
    gitPath: String,
    lastModified: LocalDateTime,
    title: String,
    abbrev: String,
    moduleType: String,
    ects: Double,
    language: String,
    duration: Int,
    season: String,
    workload: Workload,
    status: String,
    location: String,
    participantsMin: Option[Int],
    participantsMax: Option[Int],
    deContent: Content,
    enContent: Content
)

final class ModuleCompendiumTable(tag: Tag)
    extends Table[ModuleCompendiumDbEntry](tag, "metadata") {

  def id = column[UUID]("id", O.PrimaryKey)

  def gitPath = column[String]("git_path")

  def lastModified = column[LocalDateTime]("last_modified")

  def title = column[String]("title")

  def abbrev = column[String]("abbrev")

  def moduleType = column[String]("module_type")

  def ects = column[Double]("ects")

  def language = column[String]("language")

  def duration = column[Int]("duration")

  def season = column[String]("season")

  def workloadLecture = column[Int]("workload_lecture")

  def workloadSeminar = column[Int]("workload_seminar")

  def workloadPractical = column[Int]("workload_practical")

  def workloadExercise = column[Int]("workload_exercise")

  def workloadProjectSupervision = column[Int]("workload_project_supervision")

  def workloadProjectWork = column[Int]("workload_project_work")

  def workloadSelfStudy = column[Int]("workload_self_study")

  def workloadTotal = column[Int]("workload_total")

  def status = column[String]("status")

  def location = column[String]("location")

  def participantsMin = column[Option[Int]]("participants_min")

  def participantsMax = column[Option[Int]]("participants_max")

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

  override def * = (
    id,
    gitPath,
    lastModified,
    title,
    abbrev,
    moduleType,
    ects,
    language,
    duration,
    season,
    (
      workloadLecture,
      workloadSeminar,
      workloadPractical,
      workloadExercise,
      workloadProjectSupervision,
      workloadProjectWork,
      workloadSelfStudy,
      workloadTotal
    ),
    status,
    location,
    (participantsMin, participantsMax),
    (
      learningOutcomeDe,
      learningOutcomeEn
    ),
    (
      moduleContentDe,
      moduleContentEn
    ),
    (
      learningMethodsDe,
      learningMethodsEn
    ),
    (
      literatureDe,
      literatureEn
    ),
    (
      particularitiesDe,
      particularitiesEn
    )
  ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          UUID,
          String,
          LocalDateTime,
          String,
          String,
          String,
          Double,
          String,
          Int,
          String,
          (Int, Int, Int, Int, Int, Int, Int, Int),
          String,
          String,
          (Option[Int], Option[Int]),
          (String, String),
          (String, String),
          (String, String),
          (String, String),
          (String, String)
      )
  ) => ModuleCompendiumDbEntry = {
    case (
          id,
          gitPath,
          lastModified,
          title,
          abbrev,
          moduleType,
          ects,
          language,
          duration,
          season,
          (
            workloadLecture,
            workloadSeminar,
            workloadPractical,
            workloadExercise,
            workloadProjectSupervision,
            workloadProjectWork,
            workloadSelfStudy,
            workloadTotal
          ),
          status,
          location,
          (participantsMin, participantsMax),
          (
            learningOutcomeDe,
            learningOutcomeEn
          ),
          (
            moduleContentDe,
            moduleContentEn
          ),
          (
            learningMethodsDe,
            learningMethodsEn
          ),
          (
            literatureDe,
            literatureEn
          ),
          (
            particularitiesDe,
            particularitiesEn
          )
        ) =>
      ModuleCompendiumDbEntry(
        id,
        gitPath,
        lastModified,
        title,
        abbrev,
        moduleType,
        ects,
        language,
        duration,
        season,
        Workload(
          workloadLecture,
          workloadSeminar,
          workloadPractical,
          workloadExercise,
          workloadProjectSupervision,
          workloadProjectWork,
          workloadSelfStudy,
          workloadTotal
        ),
        status,
        location,
        participantsMin,
        participantsMax,
        Content(
          learningOutcomeDe,
          moduleContentDe,
          learningMethodsDe,
          literatureDe,
          particularitiesDe
        ),
        Content(
          learningOutcomeEn,
          moduleContentEn,
          learningMethodsEn,
          literatureEn,
          particularitiesEn
        )
      )
  }

  def unmapRow: ModuleCompendiumDbEntry => Option[
    (
        UUID,
        String,
        LocalDateTime,
        String,
        String,
        String,
        Double,
        String,
        Int,
        String,
        (Int, Int, Int, Int, Int, Int, Int, Int),
        String,
        String,
        (Option[Int], Option[Int]),
        (String, String),
        (String, String),
        (String, String),
        (String, String),
        (String, String)
    )
  ] =
    a =>
      Option(
        (
          a.id,
          a.gitPath,
          a.lastModified,
          a.title,
          a.abbrev,
          a.moduleType,
          a.ects,
          a.language,
          a.duration,
          a.season,
          (
            a.workload.lecture,
            a.workload.seminar,
            a.workload.practical,
            a.workload.exercise,
            a.workload.projectSupervision,
            a.workload.projectWork,
            a.workload.selfStudy,
            a.workload.total
          ),
          a.status,
          a.location,
          (a.participantsMin, a.participantsMax),
          (
            a.deContent.learningOutcome,
            a.enContent.learningOutcome
          ),
          (
            a.deContent.content,
            a.enContent.content
          ),
          (
            a.deContent.teachingAndLearningMethods,
            a.enContent.teachingAndLearningMethods
          ),
          (
            a.deContent.recommendedReading,
            a.enContent.recommendedReading
          ),
          (
            a.deContent.particularities,
            a.enContent.particularities
          )
        )
      )
}
