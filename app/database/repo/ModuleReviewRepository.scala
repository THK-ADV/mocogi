package database.repo

import database.table.ModuleReviewTable
import models.ModuleReviewStatus
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleReviewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      (UUID, ModuleReviewStatus),
      (UUID, ModuleReviewStatus),
      ModuleReviewTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  protected val tableQuery = TableQuery[ModuleReviewTable]

  override protected def retrieve(
      query: Query[ModuleReviewTable, (UUID, ModuleReviewStatus), Seq]
  ): Future[Seq[(UUID, ModuleReviewStatus)]] =
    db.run(query.result)
}
