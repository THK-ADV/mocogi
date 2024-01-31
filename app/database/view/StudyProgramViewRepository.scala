package database.view

import models.{SpecializationShort, StudyProgramView}
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

  override def name: String = "study_program_view"

  final class StudyProgramViewTable(tag: Tag)
      extends Table[StudyProgramView](tag, name) {

    def poId = column[String]("po_id")

    def studyProgramId = column[String]("sp_id")

    def studyProgramLabel = column[String]("sp_label")

    def degreeLabel = column[String]("degree_label")

    def poVersion = column[Int]("po_version")

    def specializationId = column[Option[String]]("spec_id")

    def specializationLabel = column[Option[String]]("spec_label")

    override def * = (
      poId,
      studyProgramId,
      studyProgramLabel,
      degreeLabel,
      poVersion,
      specializationId,
      specializationLabel
    ) <> (mapRow, unmapRow)

    private def mapRow: (
        (String, String, String, String, Int, Option[String], Option[String])
    ) => StudyProgramView = {
      case (
            poId,
            studyProgramId,
            studyProgramLabel,
            degreeLabel,
            poVersion,
            specializationId,
            specializationLabel
          ) =>
        StudyProgramView(
          poId,
          poVersion,
          studyProgramId,
          studyProgramLabel,
          degreeLabel,
          specializationId
            .zip(specializationLabel)
            .map((SpecializationShort.apply _).tupled)
        )
    }

    private def unmapRow: StudyProgramView => Option[
      (String, String, String, String, Int, Option[String], Option[String])
    ] = { a =>
      Option(
        (
          a.poId,
          a.studyProgramId,
          a.studyProgramLabel,
          a.degreeLabel,
          a.poVersion,
          a.specialization.map(_.id),
          a.specialization.map(_.label)
        )
      )
    }
  }

  val tableQuery = TableQuery[StudyProgramViewTable]

  def all(): Future[Seq[StudyProgramView]] =
    db.run(tableQuery.result)
}
