package database.repo

import com.google.inject.Inject
import database.table.{ModuleDraftTable, ModuleReviewTable, PersonTable, StudyProgramPersonTable}
import models.{ModuleReviewStatus, User}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleApprovalRepository @Inject() (
  val dbConfigProvider: DatabaseConfigProvider,
  implicit val ctx: ExecutionContext
)  extends HasDatabaseConfigProvider[JdbcProfile]{

  import database.table.{
    jsValueColumnType,
    moduleReviewStatusColumnType,
    universityRoleColumnType,
    userColumnType
  }
  import profile.api._

  private def tableQuery = TableQuery[ModuleReviewTable]

  private def personTable = TableQuery[PersonTable]

  private def studyProgramPersonTable = TableQuery[StudyProgramPersonTable]

  private def directorsQuery(user: User) = {
    val personQuery = personTable
      .filter(_.campusId === user.username)
      .map(_.id)
    studyProgramPersonTable.filter(_.person.in(personQuery))
  }

  private def moduleDraftTable = TableQuery[ModuleDraftTable]

  def hasPendingApproval(reviewId: UUID, user: User): Future[Boolean] = {
    val pending: ModuleReviewStatus = ModuleReviewStatus.Pending
    val spp = directorsQuery(user)
    val query = tableQuery
      .join(spp)
      .on((r, spp) =>
        r.studyProgram === spp.studyProgram && r.role === spp.role && r.id === reviewId && r.status === pending
      )
      .exists

    db.run(query.result)
  }

  def allByModulesWhereUserExists(user: User) = {
    val spp = directorsQuery(user)
    val query = tableQuery
      .joinLeft(spp)
      .on((r, spp) =>
        r.studyProgram === spp.studyProgram && r.role === spp.role
      )
      .join(
        tableQuery
          .join(spp)
          .on((r, spp) =>
            r.studyProgram === spp.studyProgram && r.role === spp.role
          )
      )
      .on(_._1.moduleDraft === _._1.moduleDraft)
      .join(moduleDraftTable)
      .on(_._1._1.moduleDraft === _.module)
      .map { case (((r, spp), _), d) =>
        (d.module, d.user, d.data, r.role, r.studyProgram, r.status, spp, r.id)
      }
      .distinctOn(a => (a._1, a._4, a._5))
    db.run(query.result)
  }
}
