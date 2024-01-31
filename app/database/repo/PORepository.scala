package database.repo

import database.table.{
  PODbEntry,
  POModificationDateDbEntry,
  POModificationDateTable,
  POTable,
  SpecializationTable
}
import models.POShort
import models.core.PO
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PORepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[POTable]

  private val poModificationDateTableQuery = TableQuery[POModificationDateTable]

  def exists(id: String): Future[Boolean] =
    db.run(tableQuery.filter(_.id === id).exists.result)

  def all(): Future[Seq[PO]] =
    retrieve(tableQuery)

  def allIds(): Future[Seq[String]] =
    db.run(tableQuery.map(_.id).result)

  def retrieve(query: Query[POTable, PODbEntry, Seq]) =
    db.run(
      query
        .joinLeft(poModificationDateTableQuery)
        .on(_.id === _.po)
        .result
        .map(_.groupBy(_._1).map { case (po, deps) =>
          val dates = deps.flatMap(_._2.map(_.date)).toList
          PO(
            po.id,
            po.version,
            po.date,
            po.dateFrom,
            po.dateTo,
            dates,
            po.studyProgram
          )
        }.toSeq)
    )

  def create(po: PO) = {
    val action = for {
      _ <- tableQuery += toDbEntry(po)
      _ <- poModificationDateTableQuery ++= po.modificationDates.map(date =>
        POModificationDateDbEntry(po.id, date)
      )
    } yield po

    db.run(action.transactionally)
  }

  def createMany(xs: List[PO]): Future[List[PODbEntry]] = {
    val pos = ListBuffer[PODbEntry]()
    val poModificationDates = ListBuffer[POModificationDateDbEntry]()

    xs.foreach { po =>
      pos += toDbEntry(po)
      po.modificationDates.foreach { date =>
        poModificationDates += POModificationDateDbEntry(po.id, date)
      }
    }

    val action = for {
      _ <- tableQuery ++= pos
      _ <- poModificationDateTableQuery ++= poModificationDates
    } yield pos.toList

    db.run(action.transactionally)
  }

  private def updateAction(po: PO) =
    (
      for {
        _ <- poModificationDateTableQuery.filter(_.po === po.id).delete
        _ <- tableQuery.filter(_.id === po.id).update(toDbEntry(po))
        _ <- poModificationDateTableQuery ++= po.modificationDates.map(date =>
          POModificationDateDbEntry(po.id, date)
        )
      } yield po
    ).transactionally

  def update(po: PO) =
    db.run(updateAction(po))

  private def toDbEntry(po: PO): PODbEntry =
    PODbEntry(
      po.id,
      po.program,
      po.version,
      po.date,
      po.dateFrom,
      po.dateTo
    )

  def allValid(date: LocalDate = LocalDate.now): Future[Seq[PO]] =
    retrieve(tableQuery.filter(_.isValid(date)))

  def allValidShort(date: LocalDate = LocalDate.now): Future[Seq[POShort]] =
    allShortQuery(tableQuery.filter(_.isValid(date)))

  private def allShortQuery(base: Query[POTable, PODbEntry, Seq]) = {
    val query = for {
      q <- base
      sp <- q.studyProgramFk
      g <- sp.gradeFk
    } yield (q.id, q.version, (sp.id, sp.deLabel, sp.enLabel, g))

    db.run(
      query
        .joinLeft(TableQuery[SpecializationTable])
        .on(_._1 === _.po)
        .result
        .map(_.map(a => POShort(a._1, a._2)))
    )
  }
}
