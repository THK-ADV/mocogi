package database.repo

import database.table.ModuleReviewTable
import models.{ModuleReview, ModuleReviewStatus}
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

  import database.table.moduleReviewStatusColumnType
  import profile.api._

  protected val tableQuery = TableQuery[ModuleReviewTable]

  def delete(moduleId: UUID): Future[Unit] =
    db.run(tableQuery.filter(_.moduleDraft === moduleId).delete.map(_ => ()))

  def deleteMany(moduleIds: Seq[UUID]): Future[Int] =
    db.run(tableQuery.filter(_.moduleDraft.inSet(moduleIds)).delete)

  def get(id: UUID) =
    db.run(tableQuery.filter(_.id === id).result.map(_.headOption))

  def getByModule(moduleId: UUID) =
    db.run(tableQuery.filter(_.moduleDraft === moduleId).result)

  def update(id: UUID, status: ModuleReviewStatus, comment: Option[String]) =
    db.run(
      tableQuery
        .filter(_.id === id)
        .map(a => (a.status, a.comment))
        .update((status, comment))
        .map(_ => ())
    )

  override protected def retrieve(
      query: Query[ModuleReviewTable, ModuleReview, Seq]
  ): Future[Seq[ModuleReview]] = db.run(query.result)
}
