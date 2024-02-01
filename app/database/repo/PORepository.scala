package database.repo

import database.table.{POTable, SpecializationTable}
import models.POShort
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

  def exists(id: String): Future[Boolean] =
    db.run(tableQuery.filter(_.id === id).exists.result)

  def allIds(): Future[Seq[String]] =
    db.run(tableQuery.map(_.id).result)

  def allValid(date: LocalDate = LocalDate.now): Future[Seq[PO]] =
    retrieve(tableQuery.filter(_.isValid(date)))

  def allValidShort(date: LocalDate = LocalDate.now): Future[Seq[POShort]] =
    allShortQuery(tableQuery.filter(_.isValid(date)))

  private def allShortQuery(base: Query[POTable, PO, Seq]) = {
    val query = for {
      q <- base
      sp <- q.studyProgramFk
      g <- sp.degreeFk
    } yield (q.id, q.version, (sp.id, sp.deLabel, sp.enLabel, g))

    db.run(
      query
        .joinLeft(TableQuery[SpecializationTable])
        .on(_._1 === _.po)
        .result
        .map(_.map(a => POShort(a._1, a._2)))
    )
  }

  override protected def retrieve(
      query: Query[POTable, PO, Seq]
  ): Future[Seq[PO]] =
    db.run(query.result)
}
