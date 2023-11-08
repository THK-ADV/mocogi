package database.repo

import database.repo.StudyProgramDirectorsRepository.StudyProgramDirector
import database.table.{POTable, StudyProgramPersonTable}
import models.UniversityRole
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object StudyProgramDirectorsRepository {
  case class StudyProgramDirector(
      directorId: String,
      campusId: Option[String],
      studyProgramAbbrev: String,
      studyProgramLabel: String,
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
      TableQuery[POTable].filter(_.abbrev.inSet(pos)).map(_.studyProgram)
    val query = for {
      a <- TableQuery[StudyProgramPersonTable]
      if a.studyProgram.in(sps) && a.role.inSet(roles)
      p <- a.personFk
      s <- a.studyProgramFk
    } yield (p.id, p.campusId, s.abbrev, s.deLabel, a.role)
    db.run(query.result.map(_.map(StudyProgramDirector.tupled)))
  }

  //  def rolesFromDirector(
  //      user: User,
  //      pos: Set[String]
  //  ): Future[Seq[UniversityRole]] = {
  //    val sps =
  //      TableQuery[POTable].filter(_.abbrev.inSet(pos)).map(_.studyProgram)
  //
  //    db.run(
  //      studyProgramPersonTableQuery
  //        .filter(a =>
  //          a.studyProgram
  //            .in(sps) && a.personFk.filter(_.campusId === user.username).exists
  //        )
  //        .map(_.role)
  //        .result
  //    )
  //  }
}
