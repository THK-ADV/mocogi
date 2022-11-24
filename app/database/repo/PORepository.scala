package database.repo

import basedata.PO
import database.table.{
  PODbEntry,
  POModificationDateDbEntry,
  POModificationDateTable,
  POTable
}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

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

  def exists(abbrev: String): Future[Boolean] =
    db.run(tableQuery.filter(_.abbrev === abbrev).exists.result)

  def all(): Future[Seq[PO]] =
    retrieve(tableQuery)

  def retrieve(query: Query[POTable, PODbEntry, Seq]) =
    db.run(
      query
        .joinLeft(poModificationDateTableQuery)
        .on(_.abbrev === _.po)
        .result
        .map(_.groupBy(_._1).map { case (po, deps) =>
          val dates = deps.flatMap(_._2.map(_.date)).toList
          PO(
            po.abbrev,
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
        POModificationDateDbEntry(po.abbrev, date)
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
        poModificationDates += POModificationDateDbEntry(po.abbrev, date)
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
        _ <- poModificationDateTableQuery.filter(_.po === po.abbrev).delete
        _ <- tableQuery.filter(_.abbrev === po.abbrev).update(toDbEntry(po))
        _ <- poModificationDateTableQuery ++= po.modificationDates.map(date =>
          POModificationDateDbEntry(po.abbrev, date)
        )
      } yield po
    ).transactionally

  def update(po: PO) =
    db.run(updateAction(po))

  def updateMany(pos: List[PO]) =
    db.run(DBIO.sequence(pos.map(updateAction)))

  private def toDbEntry(po: PO): PODbEntry =
    PODbEntry(
      po.abbrev,
      po.program,
      po.version,
      po.date,
      po.dateFrom,
      po.dateTo
    )
}
