package database.repo

import database.table.{ModuleReviewRequestTable, ModuleReviewTable}
import models.ModuleReview
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
      ModuleReview,
      ModuleReview,
      ModuleReviewTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  protected val tableQuery = TableQuery[ModuleReviewTable]

  private val requestTableQuery = TableQuery[ModuleReviewRequestTable]

  def delete(moduleId: UUID) =
    db.run(
      for {
        _ <- requestTableQuery.filter(_.review === moduleId).delete
        _ <- tableQuery.filter(_.moduleDraft === moduleId).delete
      } yield ()
    )

  override protected def retrieve(
      query: Query[ModuleReviewTable, ModuleReview, Seq]
  ): Future[Seq[ModuleReview]] = {
    db.run(
      query
        .joinLeft(requestTableQuery)
        .on(_.moduleDraft === _.review)
        .result
        .map(_.groupBy(_._1.moduleDraft).map { case (_, requests) =>
          requests.head._1.copy(requests = requests.flatMap(_._2))
        }.toSeq)
    )
  }

  override def create(input: ModuleReview): Future[ModuleReview] =
    db.run(
      DBIO
        .seq(tableQuery += input, requestTableQuery ++= input.requests)
        .transactionally
        .map(_ => input)
    )
}
