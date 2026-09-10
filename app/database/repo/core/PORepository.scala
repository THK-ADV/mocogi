package database.repo.core

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.core.POTable
import models.core.PO
import ops.single
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class PORepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[PO, POTable] {
  import profile.api.*

  protected val tableQuery = TableQuery[POTable]

  protected override def idOf(t: POTable) = t.id

  def allValid(date: LocalDate = LocalDate.now): Future[Seq[PO]] =
    db.run(tableQuery.filter(_.isValid(date)).result)

  def allExpired(date: LocalDate = LocalDate.now): Future[Seq[PO]] =
    db.run(tableQuery.filter(_.isExpired(date)).result)

  def allWithIds(pos: List[String]): Future[Seq[PO]] =
    db.run(tableQuery.filter(_.id.inSet(pos)).result)

  def get(id: String): Future[PO] =
    db.run(tableQuery.filter(_.id === id).result).single
}
