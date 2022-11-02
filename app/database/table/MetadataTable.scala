package database.table

import slick.jdbc.PostgresProfile.api._
import validator.Workload

import java.util.UUID

case class MetadataDbEntry(
    id: UUID,
    gitPath: String,
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
    participantsMax: Option[Int]
)

final class MetadataTable(tag: Tag)
    extends Table[MetadataDbEntry](tag, "metadata") {

  def id = column[UUID]("id", O.PrimaryKey)

  def gitPath = column[String]("git_path")

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
    title,
    abbrev,
    moduleType,
    ects,
    language,
    duration,
    season,
    workloadLecture,
    workloadSeminar,
    workloadPractical,
    workloadExercise,
    workloadProjectSupervision,
    workloadProjectWork,
    workloadSelfStudy,
    workloadTotal,
    status,
    location,
    participantsMin,
    participantsMax
  ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          UUID,
          String,
          String,
          String,
          String,
          Double,
          String,
          Int,
          String,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          Int,
          String,
          String,
          Option[Int],
          Option[Int]
      )
  ) => MetadataDbEntry = {
    case (
          id,
          gitPath,
          title,
          abbrev,
          moduleType,
          ects,
          language,
          duration,
          season,
          workloadLecture,
          workloadSeminar,
          workloadPractical,
          workloadExercise,
          workloadProjectSupervision,
          workloadProjectWork,
          workloadSelfStudy,
          workloadTotal,
          status,
          location,
          participantsMin,
          participantsMax
        ) =>
      MetadataDbEntry(
        id,
        gitPath,
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
        participantsMax
      )
  }

  def unmapRow: MetadataDbEntry => Option[
    (
        UUID,
        String,
        String,
        String,
        String,
        Double,
        String,
        Int,
        String,
        Int,
        Int,
        Int,
        Int,
        Int,
        Int,
        Int,
        Int,
        String,
        String,
        Option[Int],
        Option[Int]
    )
  ] =
    a =>
      Option(
        (
          a.id,
          a.gitPath,
          a.title,
          a.abbrev,
          a.moduleType,
          a.ects,
          a.language,
          a.duration,
          a.season,
          a.workload.lecture,
          a.workload.seminar,
          a.workload.practical,
          a.workload.exercise,
          a.workload.projectSupervision,
          a.workload.projectWork,
          a.workload.selfStudy,
          a.workload.total,
          a.status,
          a.location,
          a.participantsMin,
          a.participantsMax
        )
      )
}
