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

  def learningOutcomeBodyDe = column[String]("learning_outcome_body_de")

  def learningOutcomeHeaderDe = column[String]("learning_outcome_header_de")

  def learningOutcomeBodyEn = column[String]("learning_outcome_body_en")

  def learningOutcomeHeaderEn = column[String]("learning_outcome_header_en")

  def moduleContentBodyDe = column[String]("module_content_body_de")

  def moduleContentHeaderDe = column[String]("module_content_header_de")

  def moduleContentBodyEn = column[String]("module_content_body_en")

  def moduleContentHeaderEn = column[String]("module_content_header_en")

  def learningMethodsBodyDe = column[String]("learning_methods_body_de")

  def learningMethodsHeaderDe = column[String]("learning_methods_header_de")

  def learningMethodsBodyEn = column[String]("learning_methods_body_en")

  def learningMethodsHeaderEn = column[String]("learning_methods_header_en")

  def literatureBodyDe = column[String]("literature_body_de")

  def literatureHeaderDe = column[String]("literature_header_de")

  def literatureBodyEn = column[String]("literature_body_en")

  def literatureHeaderEn = column[String]("literature_header_en")

  def particularitiesBodyDe = column[String]("particularities_body_de")

  def particularitiesHeaderDe = column[String]("particularities_header_de")

  def particularitiesBodyEn = column[String]("particularities_body_en")

  def particularitiesHeaderEn = column[String]("particularities_header_en")

  def moduleTypeFk =
    foreignKey("module_type", moduleType, TableQuery[ModuleTypeTable])(_.abbrev)

  def languageFk =
    foreignKey("language", language, TableQuery[LanguageTable])(_.abbrev)

  def seasonFk =
    foreignKey("season", season, TableQuery[SeasonTable])(_.abbrev)

  def statusFk =
    foreignKey("status", status, TableQuery[StatusTable])(_.abbrev)

  def locationFk =
    foreignKey("location", location, TableQuery[LocationTable])(_.abbrev)

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
      learningOutcomeHeaderDe,
      learningOutcomeBodyDe,
      learningOutcomeHeaderEn,
      learningOutcomeBodyEn
    ),
    (
      moduleContentHeaderDe,
      moduleContentBodyDe,
      moduleContentHeaderEn,
      moduleContentBodyEn
    ),
    (
      learningMethodsHeaderDe,
      learningMethodsBodyDe,
      learningMethodsHeaderEn,
      learningMethodsBodyEn
    ),
    (
      literatureHeaderDe,
      literatureBodyDe,
      literatureHeaderEn,
      literatureBodyEn
    ),
    (
      particularitiesHeaderDe,
      particularitiesBodyDe,
      particularitiesHeaderEn,
      particularitiesBodyEn
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
          (String, String, String, String),
          (String, String, String, String),
          (String, String, String, String),
          (String, String, String, String),
          (String, String, String, String)
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
            learningOutcomeHeaderDe,
            learningOutcomeBodyDe,
            learningOutcomeHeaderEn,
            learningOutcomeBodyEn
          ),
          (
            moduleContentHeaderDe,
            moduleContentBodyDe,
            moduleContentHeaderEn,
            moduleContentBodyEn
          ),
          (
            learningMethodsHeaderDe,
            learningMethodsBodyDe,
            learningMethodsHeaderEn,
            learningMethodsBodyEn
          ),
          (
            literatureHeaderDe,
            literatureBodyDe,
            literatureHeaderEn,
            literatureBodyEn
          ),
          (
            particularitiesHeaderDe,
            particularitiesBodyDe,
            particularitiesHeaderEn,
            particularitiesBodyEn
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
          learningOutcomeHeaderDe,
          learningOutcomeBodyDe,
          moduleContentHeaderDe,
          moduleContentBodyDe,
          learningMethodsHeaderDe,
          learningMethodsBodyDe,
          literatureHeaderDe,
          literatureBodyDe,
          particularitiesHeaderDe,
          particularitiesBodyDe
        ),
        Content(
          learningOutcomeHeaderEn,
          learningOutcomeBodyEn,
          moduleContentHeaderEn,
          moduleContentBodyEn,
          learningMethodsHeaderEn,
          learningMethodsBodyEn,
          literatureHeaderEn,
          literatureBodyEn,
          particularitiesHeaderEn,
          particularitiesBodyEn
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
        (String, String, String, String),
        (String, String, String, String),
        (String, String, String, String),
        (String, String, String, String),
        (String, String, String, String)
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
            a.deContent.learningOutcomeHeader,
            a.deContent.learningOutcomeBody,
            a.enContent.learningOutcomeHeader,
            a.enContent.learningOutcomeBody
          ),
          (
            a.deContent.contentHeader,
            a.deContent.contentBody,
            a.enContent.contentHeader,
            a.enContent.contentBody
          ),
          (
            a.deContent.teachingAndLearningMethodsHeader,
            a.deContent.teachingAndLearningMethodsBody,
            a.enContent.teachingAndLearningMethodsHeader,
            a.enContent.teachingAndLearningMethodsBody
          ),
          (
            a.deContent.recommendedReadingHeader,
            a.deContent.recommendedReadingBody,
            a.enContent.recommendedReadingHeader,
            a.enContent.recommendedReadingBody
          ),
          (
            a.deContent.particularitiesHeader,
            a.deContent.particularitiesBody,
            a.enContent.particularitiesHeader,
            a.enContent.particularitiesBody
          )
        )
      )
}
