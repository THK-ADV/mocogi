package database.repo.core

import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.Inject
import database.table.core.*
import database.view.StudyProgramViewRepository
import models.core.Degree
import models.StudyProgramPrivileges
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
  import database.table.universityRoleColumnType
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

  def hasRoles(
      person: String,
      studyProgram: String,
      roles: List[UniversityRole]
  ): Future[Boolean] =
    db.run(
      studyProgramPersonTable
        .filter(a =>
          a.person === person &&
            a.studyProgram === studyProgram
            && a.role.inSet(roles)
        )
        .exists
        .result
    )

  def getStudyProgramPrivileges(person: String): Future[Iterable[StudyProgramPrivileges]] =
    db.run(
      studyProgramPersonTable
        .filter(_.person === person)
        .join(studyProgramViewTable)
        .on(_.studyProgram === _.studyProgramId)
        .result
        .map(_.groupBy(_._2.fullPoId.id).map {
          case (_, xs) =>
            assert(xs.size <= UniversityRole.all().size)
            val studyProgram = xs.head._2
            val roles        = xs.map(_._1.role)
            StudyProgramPrivileges(studyProgram, roles.toSet)
        })
    )
}
