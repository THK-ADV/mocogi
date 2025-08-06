package database.view

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.*
import models.core.Degree
import models.core.IDLabel
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.GetResult
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api.*

  private given GetResult[Option[String]] =
    GetResult(_.nextStringOption())

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

  private def intsToString(xs: List[Int]): String =
    if (xs.isEmpty) "" else xs.mkString(",")

  private def stringToInts(s: String): List[Int] =
    if (s.isEmpty) Nil
    else
      s.split(",").foldLeft(List.empty[Int]) {
        case (acc, s) =>
          s.toInt :: acc
      }

  def get(id: UUID): Future[Option[String]] = {
    val query = sql"select get_module_details(${id.toString}::uuid)".as[Option[String]].head
    db.run(query)
  }

  def allModuleCore(): Future[String] = {
    val query = sql"select * from module_core".as[String].head
    db.run(query)
  }

  def allGenericModuleOptions(id: UUID): Future[String] = {
    val query = sql"select get_generic_module_options(${id.toString}::uuid)".as[String].head
    db.run(query)
  }

  def all(): Future[Iterable[Entry]] =
    db.run(
      tableQuery.result.map(_.groupBy(_.id).map {
        case (_, deps) =>
          val moduleManagement = mutable.Set[ModuleManagement]()
          val studyPrograms =
            mutable.Set[StudyProgramModuleAssociation[Iterable[Int]]]()
          deps.foreach { dep =>
            moduleManagement.add(dep.moduleManagement)
            studyPrograms.add(
              dep.studyProgram.copy(recommendedSemester = stringToInts(dep.studyProgram.recommendedSemester))
            )
          }
          deps.head.copy(
            moduleManagement = moduleManagement.toSet,
            studyProgram = studyPrograms.toSet
          )
      })
    )

  private final class ModuleViewTable(tag: Tag) extends Table[DbEntry](tag, name) {

    import database.MyPostgresProfile.MyAPI.simpleIntListTypeMapper

    private def id                        = column[UUID]("id")
    private def title                     = column[String]("title")
    private def abbrev                    = column[String]("abbrev")
    private def ects                      = column[Double]("ects")
    private def status                    = column[String]("status")
    private def moduleManagementId        = column[String]("module_management_id")
    private def moduleManagementKind      = column[String]("module_management_kind")
    private def moduleManagementAbbrev    = column[Option[String]]("module_management_abbreviation")
    private def moduleManagementTitle     = column[String]("module_management_title")
    private def moduleManagementFirstname = column[Option[String]]("module_management_firstname")
    private def moduleManagementLastname  = column[Option[String]]("module_management_lastname")
    private def recommendedSemester       = column[List[Int]]("recommended_semester")
    private def mandatory                 = column[Boolean]("mandatory")
    private def studyProgramDeLabel       = column[String]("sp_de_label")
    private def studyProgramEnLabel       = column[String]("sp_en_label")
    private def studyProgramAbbreviation  = column[String]("sp_abbrev")
    private def studyProgramId            = column[String]("sp_id")
    private def degreeId                  = column[String]("degree_id")
    private def degreeDeLabel             = column[String]("degree_de_label")
    private def degreeEnLabel             = column[String]("degree_en_label")
    private def degreeDeDesc              = column[String]("degree_de_desc")
    private def degreeEnDesc              = column[String]("degree_en_desc")
    private def poId                      = column[String]("po_id")
    private def poVersion                 = column[Int]("po_version")
    private def specializationId          = column[Option[String]]("spec_id")
    private def specializationLabel       = column[Option[String]]("spec_label")

    override def * = (
      id,
      title,
      abbrev,
      ects,
      status,
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
            UUID,
            String,
            String,
            Double,
            String,
            (String, String, Option[String], String, Option[String], Option[String]),
            List[Int],
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
            status,
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
            studyProgramAbbreviation,
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
          status,
          ModuleManagement(
            moduleManagementId,
            moduleManagementAbbrev.getOrElse(""),
            moduleManagementKind,
            moduleManagementTitle,
            moduleManagementFirstname.getOrElse(""),
            moduleManagementLastname.getOrElse("")
          ),
          StudyProgramModuleAssociation(
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
            ),
            mandatory,
            intsToString(recommendedSemester)
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
          (String, String, Option[String], String, Option[String], Option[String]),
          List[Int],
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
          a.status,
          (
            a.moduleManagement.id,
            a.moduleManagement.kind,
            Option.when(a.moduleManagement.abbreviation.nonEmpty)(a.moduleManagement.abbreviation),
            a.moduleManagement.title,
            Option.when(a.moduleManagement.firstname.nonEmpty)(a.moduleManagement.firstname),
            Option.when(a.moduleManagement.lastname.nonEmpty)(a.moduleManagement.lastname)
          ),
          stringToInts(a.studyProgram.recommendedSemester),
          a.studyProgram.mandatory,
          a.studyProgram.studyProgram.po.id,
          a.studyProgram.studyProgram.po.version,
          a.studyProgram.studyProgram.id,
          a.studyProgram.studyProgram.deLabel,
          a.studyProgram.studyProgram.enLabel,
          a.studyProgram.studyProgram.abbreviation,
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
