package database.view

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.single
import models.core.Degree
import models.core.IDLabel
import models.FullPoId
import models.POCore
import models.StudyProgramView
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class StudyProgramViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api.*

  override def name: String = "study_program_view"

  private val notExpiredTableQuery =
    TableQuery[StudyProgramViewTable]((tag: Tag) => new StudyProgramViewTable(tag, "study_program_view_not_expired"))

  private val currentlyActiveTableQuery = TableQuery[StudyProgramViewTable]((tag: Tag) =>
    new StudyProgramViewTable(tag, "study_program_view_currently_active")
  )

  val tableQuery = notExpiredTableQuery

  /**
   * Retrieves all study programs with non-expired POs.
   * Includes POs that are currently active, future POs, and POs with no expiry date.
   *
   * @return Future sequence of study programs where PO date_to is null or >= now()
   */
  def notExpired(): Future[Seq[StudyProgramView]] =
    db.run(notExpiredTableQuery.result)

  /**
   * Retrieves study programs with currently active POs only.
   * Includes POs that have started and not yet expired.
   *
   * @return Future sequence of study programs where PO date_from <= now() <= date_to (or date_to is null)
   */
  def currentlyActive(): Future[Seq[StudyProgramView]] =
    db.run(currentlyActiveTableQuery.result)

  def getByPo(fullPoId: FullPoId): Future[StudyProgramView] =
    db.run(notExpiredTableQuery.filter(_.fullPo === fullPoId.id).result.single)

  final class StudyProgramViewTable(tag: Tag, tableName: String) extends Table[StudyProgramView](tag, tableName) {

    def fullPo = specializationId.fold(poId)(identity)

    def studyProgramDeLabel = column[String]("sp_de_label")

    def studyProgramEnLabel = column[String]("sp_en_label")

    def studyProgramId = column[String]("sp_id")

    def studyProgramAbbreviation = column[String]("sp_abbrev")

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
      studyProgramAbbreviation,
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
            studyProgramAbbreviation,
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
          studyProgramAbbreviation,
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
          a.abbreviation,
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
