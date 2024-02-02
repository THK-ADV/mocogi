package database.repo

import com.google.inject.Inject
import database.table.StudyProgramPersonTable
import database.view.StudyProgramViewRepository
import models.{StudyProgramDirector, UniversityRole}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class StudyProgramPersonRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepo: StudyProgramViewRepository,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.universityRoleColumnType
  import profile.api._

  private def studyProgramPersonTable = TableQuery[StudyProgramPersonTable]

  private def studyProgramViewTable = studyProgramViewRepo.tableQuery

  def directorsQuery(person: String) =
    for {
      q <- studyProgramPersonTable.filter(_.person === person)
      sp <- q.studyProgramFk
      g <- sp.degreeFk
    } yield (q, sp, g)

  def hasRole(
      person: String,
      studyProgram: String,
      role: UniversityRole
  ): Future[Boolean] =
    db.run(
      studyProgramPersonTable
        .filter(a =>
          a.person === person && a.studyProgram === studyProgram && a.role === role
        )
        .exists
        .result
    )

  def getDirectors(person: String): Future[Iterable[StudyProgramDirector]] =
    db.run(
      studyProgramPersonTable
        .filter(_.person === person)
        .join(studyProgramViewTable)
        .on(_.studyProgram === _.studyProgramId)
        .result
        .map(_.groupBy(_._1.person).map { case (_, xs) =>
          val person = xs.head._1
          val studyPrograms = xs.map(_._2)
          StudyProgramDirector(person.person, person.role, studyPrograms)
        })
    )
}
