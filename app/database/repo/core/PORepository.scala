package database.repo.core

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.Repository
import database.table.core.POTable
import models.core.PO
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

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

  protected override def retrieve(
      query: Query[POTable, PO, Seq]
  ): Future[Seq[PO]] =
    db.run(query.result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)
}
