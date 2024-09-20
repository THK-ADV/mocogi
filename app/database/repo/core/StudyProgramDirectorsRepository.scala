package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.StudyProgramDirectorsRepository.StudyProgramDirector
import database.table.core.POTable
import database.table.core.StudyProgramPersonTable
import models.UniversityRole
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

object StudyProgramDirectorsRepository {
  case class StudyProgramDirector(
      directorId: String,
      directorFirstname: String,
      directorLastname: String,
      studyProgramDegreeLabel: String,
      studyProgramLabel: String,
      studyProgramId: String,
      role: UniversityRole
  )
}

@Singleton
final class StudyProgramDirectorsRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.universityRoleColumnType
  import profile.api._

  def all(
      pos: Set[String],
      roles: Set[UniversityRole]
  ): Future[Seq[StudyProgramDirector]] = {
    val sps =
      TableQuery[POTable].filter(_.id.inSet(pos)).map(_.studyProgram)
    val query = for {
      a <- TableQuery[StudyProgramPersonTable]
      if a.studyProgram.in(sps) && a.role.inSet(roles)
      p <- a.personFk
      s <- a.studyProgramFk
      d <- s.degreeFk
    } yield (
      p.id,
      p.firstname,
      p.lastname,
      d.deLabel,
      s.deLabel,
      s.id,
      a.role
    )
    db.run(query.result.map(_.map(StudyProgramDirector.apply)))
  }
}
