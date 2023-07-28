package database.view

import database.table.stringToInts
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class PersonShort(
    abbrev: String,
    kind: String,
    title: String,
    firstname: String,
    lastname: String
)

case class MetadataAtomic[ModuleManagement, Semester, StudyProgram](
    id: UUID,
    title: String,
    abbrev: String,
    ects: Double,
    moduleManagement: ModuleManagement,
    studyProgram: StudyProgram,
    recommendedSemester: Semester
)
@Singleton
final class MetadataViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api._

  private type DbEntry = MetadataAtomic[
    PersonShort,
    String,
    StudyProgramAtomic
  ]

  type Entry = MetadataAtomic[
    Iterable[PersonShort],
    Iterable[Int],
    Iterable[StudyProgramAtomic]
  ]

  override def name: String = "metadata_atomic"

  private val tableQuery = TableQuery[MetadataView]

  def all(): Future[Iterable[Entry]] =
    db.run(
      tableQuery.result.map(_.groupBy(_.id).map { case (_, deps) =>
        // TODO can this be removed via query?
        val moduleManagement = mutable.HashSet[PersonShort]()
        val studyProgram = mutable.HashSet[StudyProgramAtomic]()
        deps.foreach { dep =>
          moduleManagement.add(dep.moduleManagement)
          studyProgram.add(dep.studyProgram)
        }
        deps.head.copy(
          recommendedSemester = stringToInts(deps.head.recommendedSemester),
          moduleManagement = moduleManagement,
          studyProgram = studyProgram
        )
      })
    )

  private final class MetadataView(tag: Tag) extends Table[DbEntry](tag, name) {
    private def id = column[UUID]("id")
    private def title = column[String]("title")
    private def abbrev = column[String]("abbrev")
    private def ects = column[Double]("ects")
    private def personKind = column[String]("module_management_kind")
    private def personAbbrev = column[String]("module_management_abbrev")
    private def personTitle = column[String]("module_management_title")
    private def personFirstname = column[String]("module_management_firstname")
    private def personLastname = column[String]("module_management_lastname")
    private def poAbbrev = column[String]("po_abbrev")
    private def poVersion = column[Int]("po_version")
    private def poMandatoryRecommendedSemester =
      column[String]("recommended_semester")
    private def studyProgramAbbrev = column[String]("sp_abbrev")
    private def studyProgramDeLabel = column[String]("sp_label")
    private def gradeDeLabel = column[String]("grade_label")
    private def specializationAbbrev = column[Option[String]]("spec_abbrev")
    private def specializationLabel = column[Option[String]]("spec_label")

    override def * = (
      id,
      title,
      abbrev,
      ects,
      personAbbrev,
      personKind,
      personTitle,
      personFirstname,
      personLastname,
      poAbbrev,
      poVersion,
      poMandatoryRecommendedSemester,
      studyProgramAbbrev,
      studyProgramDeLabel,
      gradeDeLabel,
      specializationAbbrev,
      specializationLabel
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
            Int,
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
            personAbbrev,
            personKind,
            personTitle,
            personFirstname,
            personLastname,
            poAbbrev,
            poVersion,
            poMandatoryRecommendedSemester,
            studyProgramAbbrev,
            studyProgramDeLabel,
            gradeDeLabel,
            specializationAbbrev,
            specializationLabel
          ) =>
        MetadataAtomic[PersonShort, String, StudyProgramAtomic](
          id,
          title,
          abbrev,
          ects,
          PersonShort(
            personAbbrev,
            personKind,
            personTitle,
            personFirstname,
            personLastname
          ),
          StudyProgramAtomic(
            poAbbrev,
            studyProgramAbbrev,
            studyProgramDeLabel,
            gradeDeLabel,
            poVersion,
            specializationAbbrev
              .zip(specializationLabel)
              .map(SpecializationShort.tupled)
          ),
          poMandatoryRecommendedSemester
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
          Int,
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
          a.moduleManagement.abbrev,
          a.moduleManagement.kind,
          a.moduleManagement.title,
          a.moduleManagement.firstname,
          a.moduleManagement.lastname,
          a.studyProgram.poAbbrev,
          a.studyProgram.version,
          a.recommendedSemester,
          a.studyProgram.studyProgramAbbrev,
          a.studyProgram.studyProgramLabel,
          a.studyProgram.grade,
          a.studyProgram.specialization.map(_.abbrev),
          a.studyProgram.specialization.map(_.label)
        )
      )
    }
  }
}
