package database.repo

import com.google.inject.Inject
import database.repo.core.StudyProgramPersonRepository
import database.table.{ModuleDraftTable, ModuleReviewTable}
import models.ModuleReviewStatus
import models.ModuleReviewStatus.Pending
import models.core.Identity
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleApprovalRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import database.table.{moduleReviewStatusColumnType, universityRoleColumnType}
  import profile.api._

  private def tableQuery = TableQuery[ModuleReviewTable]

  private def moduleDraftTable = TableQuery[ModuleDraftTable]

  def getAllStatus(moduleDraftId: UUID): Future[Seq[ModuleReviewStatus]] =
    db.run(
      tableQuery.filter(_.moduleDraft === moduleDraftId).map(_.status).result
    )

  def hasPendingApproval(reviewId: UUID, person: String): Future[Boolean] = {
    val pending: ModuleReviewStatus = Pending
    val spp = studyProgramPersonRepository.directorsQuery(person).map(_._1)
    val query = tableQuery
      .join(spp)
      .on((r, spp) =>
        r.studyProgram === spp.studyProgram && r.role === spp.role && r.id === reviewId && r.status === pending
      )
      .exists
    db.run(query.result)
  }

  def allByModulesWhereUserExists(person: String) = {
    val spp = studyProgramPersonRepository.directorsQuery(person)
    val query = tableQuery
      .joinLeft(spp)
      .on((r, spp) =>
        r.studyProgram === spp._1.studyProgram && r.role === spp._1.role
      )
      .join(
        tableQuery
          .join(spp)
          .on((r, spp) =>
            r.studyProgram === spp._1.studyProgram && r.role === spp._1.role
          )
      )
      .on(_._1.moduleDraft === _._1.moduleDraft)
      .join(moduleDraftTable)
      .on(_._1._1.moduleDraft === _.module)
      .flatMap(a =>
        a._2.authorFk.filter(_.kind === Identity.DefaultKind).map(a -> _)
      )
      .map { case ((((r, spp), _), d), p) =>
        (
          d.module,
          d.moduleTitle,
          d.moduleAbbrev,
          p,
          r.role,
          r.studyProgram,
          r.status,
          spp.map(s => (s._2.id, s._2.deLabel, s._2.enLabel, s._3)),
          r.id
        )
      }
      .distinctOn(a => (a._1, a._4, a._5))
    db.run(query.result)
  }
}
