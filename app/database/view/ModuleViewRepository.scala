package database.view

import database.table.stringToInts
import models.{
  ModuleManagement,
  ModuleView,
  SpecializationShort,
  StudyProgramModuleAssociation
}
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
  import profile.api._

  private type DbEntry = ModuleView[
    ModuleManagement,
    StudyProgramModuleAssociation[String]
  ]

  type Entry = ModuleView[
    Iterable[ModuleManagement],
    Iterable[StudyProgramModuleAssociation[Iterable[Int]]]
  ]

  override def name: String = "module_view"

  private val tableQuery = TableQuery[ModuleViewTable]

  def all(): Future[Iterable[Entry]] =
    db.run(
      tableQuery.result.map(_.groupBy(_.id).map { case (_, deps) =>
        // TODO can this be removed via query?
        val moduleManagement = mutable.HashSet[ModuleManagement]()
        val studyPrograms =
          mutable.HashSet[StudyProgramModuleAssociation[Iterable[Int]]]()
        deps.foreach { dep =>
          moduleManagement.add(dep.moduleManagement)
          studyPrograms.add(
            dep.studyProgram.copy(recommendedSemester =
              stringToInts(dep.studyProgram.recommendedSemester)
            )
          )
        }
        deps.head.copy(
          moduleManagement = moduleManagement,
          studyProgram = studyPrograms
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
      column[String]("module_management_abbrev")
    private def moduleManagementTitle =
      column[String]("module_management_title")
    private def moduleManagementFirstname =
      column[String]("module_management_firstname")
    private def moduleManagementLastname =
      column[String]("module_management_lastname")
    private def poId = column[String]("po_id")
    private def poVersion = column[Int]("po_version")
    private def studyProgramId = column[String]("sp_id")
    private def studyProgramDeLabel = column[String]("sp_label")
    private def degreeDeLabel = column[String]("degree_label")
    private def specializationId = column[Option[String]]("spec_id")
    private def specializationLabel = column[Option[String]]("spec_label")
    private def recommendedSemester = column[String]("recommended_semester")
    private def mandatory = column[Boolean]("mandatory")

    override def * = (
      id,
      title,
      abbrev,
      ects,
      moduleManagementId,
      moduleManagementKind,
      moduleManagementAbbrev,
      moduleManagementTitle,
      moduleManagementFirstname,
      moduleManagementLastname,
      poId,
      poVersion,
      studyProgramId,
      studyProgramDeLabel,
      degreeDeLabel,
      specializationId,
      specializationLabel,
      recommendedSemester,
      mandatory
    ) <> (mapRow, unmapRow)

    private def mapRow: (
        (
            UUID,
            String,
            String,
            Double,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            Int,
            String,
            String,
            String,
            Option[String],
            Option[String],
            String,
            Boolean
        )
    ) => DbEntry = {
      case (
            id,
            title,
            abbrev,
            ects,
            moduleManagementId,
            moduleManagementKind,
            moduleManagementAbbrev,
            moduleManagementTitle,
            moduleManagementFirstname,
            moduleManagementLastname,
            poId,
            poVersion,
            studyProgramId,
            studyProgramDeLabel,
            degreeDeLabel,
            specializationId,
            specializationLabel,
            recommendedSemester,
            mandatory
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
            poId,
            studyProgramId,
            studyProgramDeLabel,
            degreeDeLabel,
            poVersion,
            specializationId
              .zip(specializationLabel)
              .map((SpecializationShort.apply _).tupled),
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
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          Int,
          String,
          String,
          String,
          Option[String],
          Option[String],
          String,
          Boolean
      )
    ] = { a =>
      Option(
        (
          a.id,
          a.title,
          a.abbrev,
          a.ects,
          a.moduleManagement.id,
          a.moduleManagement.kind,
          a.moduleManagement.abbrev,
          a.moduleManagement.title,
          a.moduleManagement.firstname,
          a.moduleManagement.lastname,
          a.studyProgram.poId,
          a.studyProgram.version,
          a.studyProgram.studyProgramId,
          a.studyProgram.studyProgramLabel,
          a.studyProgram.degreeLabel,
          a.studyProgram.specialization.map(_.id),
          a.studyProgram.specialization.map(_.label),
          a.studyProgram.recommendedSemester,
          a.studyProgram.mandatory
        )
      )
    }
  }
}
