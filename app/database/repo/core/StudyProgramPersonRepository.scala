package database.repo.core

import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.Inject
import database.table.core.*
import database.view.StudyProgramViewRepository
import models.core.Degree
import models.StudyProgramPrivileges
import models.StudyProgramView
import models.UniversityRole
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class StudyProgramPersonRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepo: StudyProgramViewRepository,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  private def studyProgramPersonTable = TableQuery[StudyProgramPersonTable]

  private def studyProgramViewTable = studyProgramViewRepo.tableQuery

  def directorsQuery(person: String): Query[
    (StudyProgramPersonTable, StudyProgramTable, DegreeTable),
    (StudyProgramPersonDbEntry, StudyProgramDbEntry, Degree),
    Seq
  ] =
    for {
      q  <- studyProgramPersonTable.filter(_.person === person)
      sp <- q.studyProgramFk
      g  <- sp.degreeFk
    } yield (q, sp, g)

  def directorsQuery(): Query[
    (StudyProgramPersonTable, StudyProgramTable, DegreeTable),
    (StudyProgramPersonDbEntry, StudyProgramDbEntry, Degree),
    Seq
  ] =
    for {
      q  <- studyProgramPersonTable
      sp <- q.studyProgramFk
      g  <- sp.degreeFk
    } yield (q, sp, g)

  def studyProgramIdsForPOs(pos: Set[String]): Future[Seq[String]] =
    db.run(studyProgramViewTable.filter(_.poId.inSet(pos)).map(_.studyProgramId).distinct.result)

  /**
   * Retrieves all study programs that do not have a specialization
   */
  def getAllStudyProgramsWithoutSpecialization(): Future[Seq[StudyProgramView]] =
    db.run(studyProgramViewTable.filter(_.specializationId.isEmpty).result)

  /**
   * Retrieves all study programs by POs that do not have a specialization
   */
  def getAllStudyProgramsWithoutSpecializationForPOs(pos: Set[String]): Future[Seq[StudyProgramView]] =
    db.run(studyProgramViewTable.filter(s => s.specializationId.isEmpty && s.poId.inSet(pos)).result)

  /**
   * Retrieves the study program privileges for a given person
   */
  def getStudyProgramPrivilegesForPerson(person: String): Future[Iterable[StudyProgramPrivileges]] =
    db.run(
      studyProgramPersonTable
        .filter(_.person === person)
        .join(studyProgramViewTable.filter(_.specializationId.isEmpty))
        .on(_.studyProgram === _.studyProgramId)
        .result
        .map(_.groupBy(_._2.fullPoId.id).map {
          case (_, (xs: Seq[(StudyProgramPersonDbEntry, StudyProgramView)])) =>
            val studyProgram = xs.head._2
            val roles        = xs.map(_._1.role).toSet
            val canCreate    = roles.contains(UniversityRole.PAV)
            StudyProgramPrivileges(studyProgram, canCreate, true)
        })
    )
}
