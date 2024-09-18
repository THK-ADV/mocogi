package database.view

import database.table.stringToInts
import models.*
import models.core.{Degree, IDLabel}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api.*

  private type DbEntry = ModuleView[
    ModuleManagement,
    StudyProgramModuleAssociation[String]
  ]

  type Entry = ModuleView[
    Set[ModuleManagement],
    Set[StudyProgramModuleAssociation[Iterable[Int]]]
  ]

  override def name: String = "module_view"

  private val tableQuery = TableQuery[ModuleViewTable]

  def all(): Future[Iterable[Entry]] =
    db.run(
      tableQuery.result.map(_.groupBy(_.id).map { case (_, deps) =>
        val moduleManagement = mutable.Set[ModuleManagement]()
        val studyPrograms =
          mutable.Set[StudyProgramModuleAssociation[Iterable[Int]]]()
        deps.foreach { dep =>
          moduleManagement.add(dep.moduleManagement)
          studyPrograms.add(
            dep.studyProgram.copy(recommendedSemester =
              stringToInts(dep.studyProgram.recommendedSemester)
            )
          )
        }
        deps.head.copy(
          moduleManagement = moduleManagement.toSet,
          studyProgram = studyPrograms.toSet
        )
      })
    )

  private final class ModuleViewTable(tag: Tag)
      extends Table[DbEntry](tag, name) {
    private def id = column[UUID]("id")
    private def title = column[String]("title")
    private def abbrev = column[String]("abbrev")
    private def ects = column[Double]("ects")
    private def moduleManagementId = column[String]("module_management_id")
    private def moduleManagementKind = column[String]("module_management_kind")
    private def moduleManagementAbbrev =
      column[String]("module_management_abbreviation")
    private def moduleManagementTitle =
      column[String]("module_management_title")
    private def moduleManagementFirstname =
      column[String]("module_management_firstname")
    private def moduleManagementLastname =
      column[String]("module_management_lastname")
    private def recommendedSemester = column[String]("recommended_semester")
    private def mandatory = column[Boolean]("mandatory")
    private def studyProgramDeLabel = column[String]("sp_de_label")
    private def studyProgramEnLabel = column[String]("sp_en_label")
    private def studyProgramId = column[String]("sp_id")
    private def degreeId = column[String]("degree_id")
    private def degreeDeLabel = column[String]("degree_de_label")
    private def degreeEnLabel = column[String]("degree_en_label")
    private def degreeDeDesc = column[String]("degree_de_desc")
    private def degreeEnDesc = column[String]("degree_en_desc")
    private def poId = column[String]("po_id")
    private def poVersion = column[Int]("po_version")
    private def specializationId = column[Option[String]]("spec_id")
    private def specializationLabel = column[Option[String]]("spec_label")

    override def * = (
      id,
      title,
      abbrev,
      ects,
      (
        moduleManagementId,
        moduleManagementKind,
        moduleManagementAbbrev,
        moduleManagementTitle,
        moduleManagementFirstname,
        moduleManagementLastname
      ),
      recommendedSemester,
      mandatory,
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
            UUID,
            String,
            String,
            Double,
            (String, String, String, String, String, String),
            String,
            Boolean,
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
    ) => DbEntry = {
      case (
            id,
            title,
            abbrev,
            ects,
            (
              moduleManagementId,
              moduleManagementKind,
              moduleManagementAbbrev,
              moduleManagementTitle,
              moduleManagementFirstname,
              moduleManagementLastname
            ),
            recommendedSemester,
            mandatory,
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
        ModuleView[ModuleManagement, StudyProgramModuleAssociation[String]](
          id,
          title,
          abbrev,
          ects,
          ModuleManagement(
            moduleManagementId,
            moduleManagementAbbrev,
            moduleManagementKind,
            moduleManagementTitle,
            moduleManagementFirstname,
            moduleManagementLastname
          ),
          StudyProgramModuleAssociation(
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
            ),
            mandatory,
            recommendedSemester
          )
        )
    }

    private def unmapRow: DbEntry => Option[
      (
          UUID,
          String,
          String,
          Double,
          (String, String, String, String, String, String),
          String,
          Boolean,
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
          a.id,
          a.title,
          a.abbrev,
          a.ects,
          (
            a.moduleManagement.id,
            a.moduleManagement.kind,
            a.moduleManagement.abbreviation,
            a.moduleManagement.title,
            a.moduleManagement.firstname,
            a.moduleManagement.lastname
          ),
          a.studyProgram.recommendedSemester,
          a.studyProgram.mandatory,
          a.studyProgram.studyProgram.po.id,
          a.studyProgram.studyProgram.po.version,
          a.studyProgram.studyProgram.id,
          a.studyProgram.studyProgram.deLabel,
          a.studyProgram.studyProgram.enLabel,
          a.studyProgram.studyProgram.degree.id,
          a.studyProgram.studyProgram.degree.deLabel,
          a.studyProgram.studyProgram.degree.enLabel,
          a.studyProgram.studyProgram.degree.deDesc,
          a.studyProgram.studyProgram.degree.enDesc,
          a.studyProgram.studyProgram.specialization.map(_.id),
          a.studyProgram.studyProgram.specialization.map(_.deLabel)
        )
      )
    }
  }
}
