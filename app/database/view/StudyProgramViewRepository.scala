package database.view

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class SpecializationShort(abbrev: String, label: String)

case class StudyProgramAtomic(
    poAbbrev: String,
    studyProgramAbbrev: String,
    studyProgramLabel: String,
    grade: String,
    version: Int,
    specialization: Option[SpecializationShort]
)

@Singleton
final class StudyProgramViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api._

  override def name: String = "study_program_atomic"

  final class StudyProgramView(tag: Tag)
      extends Table[StudyProgramAtomic](tag, name) {

    def poAbbrev = column[String]("po_abbrev")

    def studyProgramAbbrev = column[String]("sp_abbrev")

    def studyProgramLabel = column[String]("sp_label")

    def grade = column[String]("grade_label")

    def version = column[Int]("po_version")

    def specializationAbbrev = column[Option[String]]("spec_abbrev")

    def specializationLabel = column[Option[String]]("spec_label")

    override def * = (
      poAbbrev,
      studyProgramAbbrev,
      studyProgramLabel,
      grade,
      version,
      specializationAbbrev,
      specializationLabel
    ) <> (mapRow, unmapRow)
  }

  val tableQuery = TableQuery[StudyProgramView]

  def all(): Future[Seq[StudyProgramAtomic]] =
    db.run(tableQuery.result)

  def mapRow: (
      (String, String, String, String, Int, Option[String], Option[String])
  ) => StudyProgramAtomic = {
    case (
          poAbbrev,
          studyProgramAbbrev,
          studyProgramLabel,
          grade,
          version,
          specializationAbbrev,
          specializationLabel
        ) =>
      StudyProgramAtomic(
        poAbbrev,
        studyProgramAbbrev,
        studyProgramLabel,
        grade,
        version,
        specializationAbbrev
          .zip(specializationLabel)
          .map(SpecializationShort.tupled)
      )
  }

  def unmapRow: StudyProgramAtomic => Option[
    (String, String, String, String, Int, Option[String], Option[String])
  ] = { a =>
    Option(
      (
        a.poAbbrev,
        a.studyProgramAbbrev,
        a.studyProgramLabel,
        a.grade,
        a.version,
        a.specialization.map(_.abbrev),
        a.specialization.map(_.label)
      )
    )
  }
}
