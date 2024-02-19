package database.repo.core

import database.repo.Repository
import database.table.core.POTable
import models.core.PO
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class PORepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with Repository[PO, PO, POTable] {
  import profile.api._

  protected val tableQuery = TableQuery[POTable]

  def allIds(): Future[Seq[String]] =
    db.run(tableQuery.map(_.id).result)

  def allValid(date: LocalDate = LocalDate.now): Future[Seq[PO]] =
    retrieve(tableQuery.filter(_.isValid(date)))

  override protected def retrieve(
      query: Query[POTable, PO, Seq]
  ): Future[Seq[PO]] =
    db.run(query.result)
}
