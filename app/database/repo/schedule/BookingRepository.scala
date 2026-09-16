package database.repo.schedule

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.schedule.BookingDbEntry
import database.table.schedule.BookingTable
import models.schedule.BookingKind
import models.schedule.BookingProtocol
import models.schedule.ScheduleEntrySeriesId
import models.schedule.SeriesOccurrence
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class BookingRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.given_BaseColumnType_BookingKind
  import database.table.scheduleEntrySeriesIdColumnType
  import database.MyPostgresProfile.MyAPI.setUUIDArray
  import profile.api.*

  private val tableQuery = TableQuery[BookingTable]

  def bookingsByRange(kind: BookingKind, from: Timestamp, to: Timestamp): Future[String] =
    db.run(sql"select schedule.get_bookings(${kind.id}::text, $from::timestamptz, $to::timestamptz)".as[String].head)

  // Serialize writes to a series, including inserts where no row exists to lock yet.
  private def lockSeries(id: ScheduleEntrySeriesId) =
    sql"select pg_advisory_xact_lock(hashtextextended(${"booking:" + id.value}, 0))".as[String].map(_ => ())

  private def enriched(ids: List[UUID]) = sql"select schedule.get_bookings($ids)".as[String].head

  def create(entries: List[BookingProtocol], createdBy: String): Future[String] = {
    val groups = entries.groupBy(_.seriesId)
    if groups.values.exists(xs => xs.exists(_.kind != xs.head.kind)) then
      return Future.failed(new IllegalArgumentException("all bookings in a series must have the same kind"))
    val query = for {
      _        <- DBIO.sequence(groups.keys.toSeq.sortBy(_.value.toString).map(lockSeries))
      existing <- tableQuery.filter(_.seriesId.inSet(groups.keys)).map(e => (e.seriesId, e.kind)).distinct.result
      _        <-
        if existing.forall((id, kind) => groups(id).head.kind == kind) then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("all bookings in a series must have the same kind"))
      now       = Instant.now()
      dbEntries = entries.map(p => toDbEntry(UUID.randomUUID(), p.seriesId, p, createdBy, now))
      _    <- tableQuery ++= dbEntries
      json <- enriched(dbEntries.map(_.id))
    } yield json
    db.run(query.transactionally)
  }

  private def anchor(id: UUID) =
    tableQuery.filter(_.id === id).result.headOption.flatMap {
      case Some(entry) => lockSeries(entry.seriesId).map(_ => entry)
      case None        => DBIO.failed(new NoSuchElementException("booking not found"))
    }

  def update(id: UUID, booking: BookingProtocol): Future[String] = {
    val query = for {
      entry     <- anchor(id)
      otherKind <- tableQuery
        .filter(e => e.seriesId === entry.seriesId && e.id =!= id && e.kind =!= booking.kind)
        .exists
        .result
      _ <-
        if !otherKind then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("use a series update to change the kind of a series"))
      count <- tableQuery
        .filter(_.id === id)
        .update(toDbEntry(id, entry.seriesId, booking, entry.createdBy, Instant.now()))
      _    <- if count == 1 then DBIO.unit else DBIO.failed(new NoSuchElementException("booking not found"))
      json <- enriched(List(id))
    } yield json
    db.run(query.transactionally)
  }

  def updateSeries(id: UUID, booking: BookingProtocol): Future[String] = {
    val query = for {
      entry <- anchor(id)
      _     <-
        if entry.seriesId == booking.seriesId then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("seriesId does not match anchor entry"))
      entries <- tableQuery.filter(_.seriesId === entry.seriesId).forUpdate.result
      now      = Instant.now()
      setTimes = ScheduleEntryRepository.setSeriesTimes(booking.start, booking.end)
      updated  = entries.map { old =>
        val (start, end) = setTimes(old.start, old.end)
        old.copy(
          kind = booking.kind,
          title = booking.title,
          note = booking.note,
          rooms = booking.rooms,
          lecturer = booking.lecturer,
          start = start,
          end = end,
          module = booking.module,
          courseType = booking.courseType,
          po = booking.po,
          updatedAt = now
        )
      }
      _ <-
        if updated.forall(e => BookingProtocol.validTimes(e.start, e.end)) then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("series times must end after start on the same Berlin day"))
      _    <- DBIO.sequence(updated.map(e => tableQuery.filter(_.id === e.id).update(e)))
      json <- enriched(updated.iterator.map(_.id).toList)
    } yield json
    db.run(query.transactionally)
  }

  def hasSeries(seriesId: ScheduleEntrySeriesId): Future[Seq[SeriesOccurrence]] =
    db.run(tableQuery.filter(_.seriesId === seriesId).map(e => (e.id, e.start, e.end)).result).map {
      case entries if entries.size <= 1 => Seq.empty
      case entries                      => entries.map((id, start, end) => SeriesOccurrence(id, start, end))
    }

  def delete(id: UUID): Future[Boolean] =
    db.run(tableQuery.filter(_.id === id).delete).map(_ == 1)

  private def toDbEntry(
      id: UUID,
      seriesId: ScheduleEntrySeriesId,
      p: BookingProtocol,
      createdBy: String,
      updatedAt: Instant
  ): BookingDbEntry =
    BookingDbEntry(
      id,
      p.kind,
      seriesId,
      p.title,
      p.note,
      p.rooms,
      p.lecturer,
      p.start,
      p.end,
      p.module,
      p.courseType,
      p.po,
      createdBy,
      updatedAt
    )
}
