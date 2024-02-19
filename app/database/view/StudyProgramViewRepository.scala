package database.view

import models.core.{Degree, IDLabel}
import models.{POCore, StudyProgramView}
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

  val tableQuery = TableQuery[StudyProgramViewTable]

  def all(): Future[Seq[StudyProgramView]] =
    db.run(tableQuery.result)

  final class StudyProgramViewTable(tag: Tag)
      extends Table[StudyProgramView](tag, name) {

    def fullPo = specializationId.fold(poId)(identity)

    def studyProgramDeLabel = column[String]("sp_de_label")

    def studyProgramEnLabel = column[String]("sp_en_label")

    def studyProgramId = column[String]("sp_id")

    def degreeId = column[String]("degree_id")

    def degreeDeLabel = column[String]("degree_de_label")

    def degreeEnLabel = column[String]("degree_en_label")

    def degreeDeDesc = column[String]("degree_de_desc")

    def degreeEnDesc = column[String]("degree_en_desc")

    def poId = column[String]("po_id")

    def poVersion = column[Int]("po_version")

    def specializationId = column[Option[String]]("spec_id")

    def specializationLabel = column[Option[String]]("spec_label")

    override def * = (
      poId,
      poVersion,
      studyProgramId,
      studyProgramDeLabel,
      studyProgramEnLabel,
      degreeId,
      degreeDeLabel,
      degreeEnLabel,
      degreeDeDesc,
      degreeEnDesc,
      specializationId,
      specializationLabel
    ) <> (mapRow, unmapRow)

    private def mapRow: (
        (
            String,
            Int,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            Option[String],
            Option[String]
        )
    ) => StudyProgramView = {
      case (
            poId,
            poVersion,
            studyProgramId,
            studyProgramDeLabel,
            studyProgramEnLabel,
            degreeId,
            degreeDeLabel,
            degreeEnLabel,
            degreeDeDesc,
            degreeEnDesc,
            specializationId,
            specializationLabel
          ) =>
        StudyProgramView(
          studyProgramId,
          studyProgramDeLabel,
          studyProgramEnLabel,
          POCore(poId, poVersion),
          Degree(
            degreeId,
            degreeDeLabel,
            degreeDeDesc,
            degreeEnLabel,
            degreeEnDesc
          ),
          specializationId
            .zip(specializationLabel)
            .map(s => IDLabel(s._1, s._2, s._2))
        )
    }

    private def unmapRow: StudyProgramView => Option[
      (
          String,
          Int,
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          Option[String],
          Option[String]
      )
    ] = { a =>
      Option(
        (
          a.po.id,
          a.po.version,
          a.id,
          a.deLabel,
          a.enLabel,
          a.degree.id,
          a.degree.deLabel,
          a.degree.enLabel,
          a.degree.deDesc,
          a.degree.enDesc,
          a.specialization.map(_.id),
          a.specialization.map(_.deLabel)
        )
      )
    }
  }
}
