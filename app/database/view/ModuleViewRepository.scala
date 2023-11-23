package database.view

import database.table.stringToInts
import models.SpecializationShort
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class StudyProgramModuleAssociation[Semester](
    poAbbrev: String,
    studyProgramAbbrev: String,
    studyProgramLabel: String,
    grade: String,
    version: Int,
    specialization: Option[SpecializationShort],
    mandatory: Boolean,
    recommendedSemester: Semester
)

case class PersonShort(
    id: String,
    abbrev: String,
    kind: String,
    title: String,
    firstname: String,
    lastname: String
)

case class ModuleAtomic[ModuleManagement, StudyProgram](
    id: UUID,
    title: String,
    abbrev: String,
    ects: Double,
    moduleManagement: ModuleManagement,
    studyProgram: StudyProgram
)
@Singleton
final class ModuleViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api._

  private type DbEntry = ModuleAtomic[
    PersonShort,
    StudyProgramModuleAssociation[String]
  ]

  type Entry = ModuleAtomic[
    Iterable[PersonShort],
    Iterable[StudyProgramModuleAssociation[Iterable[Int]]]
  ]

  override def name: String = "module_atomic"

  private val tableQuery = TableQuery[ModuleView]

  def all(): Future[Iterable[Entry]] =
    db.run(
      tableQuery.result.map(_.groupBy(_.id).map { case (_, deps) =>
        // TODO can this be removed via query?
        val moduleManagement = mutable.HashSet[PersonShort]()
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

  private final class ModuleView(tag: Tag) extends Table[DbEntry](tag, name) {
    private def id = column[UUID]("id")
    private def title = column[String]("title")
    private def abbrev = column[String]("abbrev")
    private def ects = column[Double]("ects")
    private def personId = column[String]("module_management_id")
    private def personKind = column[String]("module_management_kind")
    private def personAbbrev = column[String]("module_management_abbrev")
    private def personTitle = column[String]("module_management_title")
    private def personFirstname = column[String]("module_management_firstname")
    private def personLastname = column[String]("module_management_lastname")
    private def poAbbrev = column[String]("po_abbrev")
    private def poVersion = column[Int]("po_version")
    private def studyProgramAbbrev = column[String]("sp_abbrev")
    private def studyProgramDeLabel = column[String]("sp_label")
    private def gradeDeLabel = column[String]("grade_label")
    private def specializationAbbrev = column[Option[String]]("spec_abbrev")
    private def specializationLabel = column[Option[String]]("spec_label")
    private def recommendedSemester = column[String]("recommended_semester")
    private def mandatory = column[Boolean]("mandatory")

    override def * = (
      id,
      title,
      abbrev,
      ects,
      personId,
      personKind,
      personAbbrev,
      personTitle,
      personFirstname,
      personLastname,
      poAbbrev,
      poVersion,
      studyProgramAbbrev,
      studyProgramDeLabel,
      gradeDeLabel,
      specializationAbbrev,
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
            personId,
            personKind,
            personAbbrev,
            personTitle,
            personFirstname,
            personLastname,
            poAbbrev,
            poVersion,
            studyProgramAbbrev,
            studyProgramDeLabel,
            gradeDeLabel,
            specializationAbbrev,
            specializationLabel,
            recommendedSemester,
            mandatory
          ) =>
        ModuleAtomic[PersonShort, StudyProgramModuleAssociation[String]](
          id,
          title,
          abbrev,
          ects,
          PersonShort(
            personId,
            personAbbrev,
            personKind,
            personTitle,
            personFirstname,
            personLastname
          ),
          StudyProgramModuleAssociation(
            poAbbrev,
            studyProgramAbbrev,
            studyProgramDeLabel,
            gradeDeLabel,
            poVersion,
            specializationAbbrev
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
          a.studyProgram.poAbbrev,
          a.studyProgram.version,
          a.studyProgram.studyProgramAbbrev,
          a.studyProgram.studyProgramLabel,
          a.studyProgram.grade,
          a.studyProgram.specialization.map(_.abbrev),
          a.studyProgram.specialization.map(_.label),
          a.studyProgram.recommendedSemester,
          a.studyProgram.mandatory
        )
      )
    }
  }
}
