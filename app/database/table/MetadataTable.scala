package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class MetadataTable(tag: Tag)
    extends Table[MetadataDbEntry](tag, "metadata") {

  def id = column[UUID]("id", O.PrimaryKey)
  def gitPath = column[String]("git_path")
  def title = column[String]("title")
  def abbrev = column[String]("abbrev")
  def moduleType = column[String]("module_type")

  def children = column[Option[String]]("children")
  def parent = column[Option[String]]("parent")

  def credits = column[Double]("credits")
  def language = column[String]("language")
  def duration = column[Int]("duration")
  def recommendedSemester = column[Int]("recommended_semester")
  def season = column[String]("season")

  def workloadTotal = column[Int]("workload_total")
  def workloadLecture = column[Int]("workload_lecture")
  def workloadSeminar = column[Int]("workload_seminar")
  def workloadPractical = column[Int]("workload_practical")
  def workloadExercise = column[Int]("workload_exercise")
  def workloadSelfStudy = column[Int]("workload_self_study")

  def recommendedPrerequisites = column[String]("recommended-prerequisites")
  def requiredPrerequisites = column[String]("required-prerequisites")

  def status = column[String]("status")
  def location = column[String]("location")

  def po = column[String]("po")

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
    (children, parent),
    credits,
    language,
    duration,
    recommendedSemester,
    season,
    (
      workloadTotal,
      workloadLecture,
      workloadSeminar,
      workloadPractical,
      workloadExercise,
      workloadSelfStudy
    ),
    recommendedPrerequisites,
    requiredPrerequisites,
    status,
    location,
    po
  ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          UUID,
          String,
          String,
          String,
          String,
          (Option[String], Option[String]),
          Double,
          String,
          Int,
          Int,
          String,
          (Int, Int, Int, Int, Int, Int),
          String,
          String,
          String,
          String,
          String
      )
  ) => MetadataDbEntry = {
    case (
          id,
          gitPath,
          title,
          abbrev,
          moduleType,
          (children, parent),
          credits,
          language,
          duration,
          recommendedSemester,
          season,
          (
            workloadTotal,
            workloadLecture,
            workloadSeminar,
            workloadPractical,
            workloadExercise,
            workloadSelfStudy
          ),
          recommendedPrerequisites,
          requiredPrerequisites,
          status,
          location,
          po
        ) =>
      MetadataDbEntry(
        id,
        gitPath,
        title,
        abbrev,
        moduleType,
        children,
        parent,
        credits,
        language,
        duration,
        recommendedSemester,
        season,
        workloadTotal,
        workloadLecture,
        workloadSeminar,
        workloadPractical,
        workloadExercise,
        workloadSelfStudy,
        recommendedPrerequisites,
        requiredPrerequisites,
        status,
        location,
        po
      )
  }

  def unmapRow: MetadataDbEntry => Option[
    (
        UUID,
        String,
        String,
        String,
        String,
        (Option[String], Option[String]),
        Double,
        String,
        Int,
        Int,
        String,
        (Int, Int, Int, Int, Int, Int),
        String,
        String,
        String,
        String,
        String
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
          (a.children, a.parent),
          a.credits,
          a.language,
          a.duration,
          a.recommendedSemester,
          a.season,
          (
            a.workloadTotal,
            a.workloadLecture,
            a.workloadSeminar,
            a.workloadPractical,
            a.workloadExercise,
            a.workloadSelfStudy
          ),
          a.recommendedPrerequisites,
          a.requiredPrerequisites,
          a.status,
          a.location,
          a.po
        )
      )
}
