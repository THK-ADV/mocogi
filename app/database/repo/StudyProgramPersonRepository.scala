package database.repo

import com.google.inject.Inject
import database.table.{POTable, SpecializationTable, StudyProgramPersonTable}
import models.{PoSpec, StudyProgramDirector, StudyProgramShort, UniversityRole}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class StudyProgramPersonRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.universityRoleColumnType
  import profile.api._

  private def studyProgramPersonTable = TableQuery[StudyProgramPersonTable]

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

  def getDirectors(person: String): Future[Iterable[StudyProgramDirector]] = {
    val query = for {
      q <- studyProgramPersonTable.filter(_.person === person)
      sp <- q.studyProgramFk
      g <- sp.degreeFk
    } yield (q.role, (sp.id, sp.deLabel, sp.enLabel, g))
    val action = query
      .join(
        TableQuery[POTable]
          .filter(_.isValid())
          .map(a => (a.id, a.version, a.studyProgram))
      )
      .on(_._2._1 === _._3)
      .joinLeft(TableQuery[SpecializationTable])
      .on(_._2._1 === _.po)
      .result
      .map(_.groupBy(_._1._1._2._1).map { case (_, xs) =>
        val (role, spg) = xs.head._1._1
        val pos =
          xs.map(a => PoSpec(a._1._2._1, a._1._2._2, a._2.map(_.toShort)))
        StudyProgramDirector(
          person,
          role,
          StudyProgramShort(spg),
          pos
        )
      })
    db.run(action)
  }
}
