package database.view

import models.{SpecializationShort, StudyProgramAtomic}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

    def poId = column[String]("po_id")

    def studyProgramId = column[String]("sp_id")

    def studyProgramLabel = column[String]("sp_label")

    def gradeLabel = column[String]("grade_label")

    def poVersion = column[Int]("po_version")

    def specializationId = column[Option[String]]("spec_id")

    def specializationLabel = column[Option[String]]("spec_label")

    override def * = (
      poId,
      studyProgramId,
      studyProgramLabel,
      gradeLabel,
      poVersion,
      specializationId,
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
          poId,
          studyProgramId,
          studyProgramLabel,
          grade,
          poVersion,
          specializationId,
          specializationLabel
        ) =>
      StudyProgramAtomic(
        poId,
        poVersion,
        studyProgramId,
        studyProgramLabel,
        grade,
        specializationId
          .zip(specializationLabel)
          .map((SpecializationShort.apply _).tupled)
      )
  }

  def unmapRow: StudyProgramAtomic => Option[
    (String, String, String, String, Int, Option[String], Option[String])
  ] = { a =>
    Option(
      (
        a.poId,
        a.studyProgramId,
        a.studyProgramLabel,
        a.gradeLabel,
        a.poVersion,
        a.specialization.map(_.id),
        a.specialization.map(_.label)
      )
    )
  }
}
