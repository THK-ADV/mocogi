package database.repo

import database.table.ModuleReviewRequestTable
import models.{ModuleReviewStatus, User}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleReviewRequestRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      (UUID, UUID, Boolean),
      (UUID, UUID, Boolean),
      ModuleReviewRequestTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import database.table.{moduleReviewStatusColumnType, userColumnType}
  import profile.api._

  protected val tableQuery = TableQuery[ModuleReviewRequestTable]

  override protected def retrieve(
      query: Query[ModuleReviewRequestTable, (UUID, UUID, Boolean), Seq]
  ): Future[Seq[(UUID, UUID, Boolean)]] =
    db.run(query.result)

  def allFromUser(
      user: User
  ): Future[Seq[(UUID, ModuleReviewStatus, Boolean)]] =
    db.run(
      (for {
        q <- tableQuery
        reviewer <- q.reviewerFk if reviewer.user === user
        review <- q.reviewFk
      } yield (review.moduleDraft, review.status, q.approved)).result
    )
}
