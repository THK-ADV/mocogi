package database.repo.schedule

import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.schedule.ScheduleEntryDbEntry
import database.table.schedule.ScheduleEntryTable
import models.schedule.ScheduleEntryProtocol
import models.schedule.ScheduleEntrySeriesId
import models.schedule.SeriesOccurrence
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ScheduleEntryRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.given_BaseColumnType_CourseType
  import database.table.scheduleEntrySeriesIdColumnType
  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.setUUIDArray
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper
  import profile.api.*

  private val tableQuery = TableQuery[ScheduleEntryTable]

  /**
   * Retrieves all schedule entries within the given time range as a JSON string.
   *
   * @param from start of the range (inclusive)
   * @param to end of the range (exclusive)
   * @return JSON string of matching schedule entries
   */
  def scheduleEntriesByRange(from: Timestamp, to: Timestamp) = {
    val query = sql"select schedule.get_schedule_entries($from, $to)".as[String].head
    db.run(query)
  }

  /**
   * Inserts the given schedule entries and returns them as a JSON string.
   * Explicit creation sets sourcePlanDraft and sourceScheduleEntryDraft to None.
   *
   * @param entries list of schedule entries to insert
   * @return JSON string of the created schedule entries
   */
  def create(entries: List[ScheduleEntryProtocol]): Future[String] = {
    val dbEntries = entries.map(toDbEntry)
    val query     = for {
      _  <- tableQuery ++= dbEntries
      xs <- sql"select schedule.get_schedule_entries(${dbEntries.map(_.id)})".as[String].head
    } yield xs
    db.run(query.transactionally)
  }

  /**
   * Updates an existing schedule entry and returns it as a JSON string.
   * All properties except seriesId, sourcePlanDraft, and sourceScheduleEntryDraft are updatable.
   *
   * @param id the ID of the entry to update
   * @param s the schedule entry with updated values
   * @return JSON string of the updated schedule entry
   */
  def update(id: UUID, s: ScheduleEntryProtocol): Future[String] = {
    val query = for {
      _ <- tableQuery
        .filter(_.id === id)
        .map(e => (e.module, e.courseType, e.rooms, e.lecturer, e.start, e.end, e.po))
        .update((s.module, s.courseType, s.rooms, s.lecturer, s.start, s.end, s.po))
      xs <- sql"select schedule.get_schedule_entries(${List(id)})".as[String].head
    } yield xs
    db.run(query.transactionally)
  }

  /**
   * Updates all schedule entries in the same series as `anchorId` and returns them as a JSON string.
   * The edited anchor entry defines the local start and end times for every entry in the series.
   *
   * @param anchorId the edited entry used to identify the series
   * @param s the updated anchor entry payload
   * @return JSON string of the updated schedule entries
   */
  def updateSeries(anchorId: UUID, s: ScheduleEntryProtocol): Future[String] = {
    val query = for {
      anchor <- tableQuery.filter(_.id === anchorId).forUpdate.result.headOption.flatMap {
        case Some(entry) => DBIO.successful(entry)
        case None        => DBIO.failed(new NoSuchElementException("schedule entry not found"))
      }
      _ <-
        if anchor.seriesId == s.seriesId then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("seriesId does not match anchor entry"))
      _ <-
        if s.end.isAfter(s.start) then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("end must be after start"))
      entries <- tableQuery.filter(_.seriesId === anchor.seriesId).forUpdate.result
      setSeriesTimes = ScheduleEntryRepository.setSeriesTimes(s.start, s.end)
      updated        = entries.map { entry =>
        val (nextStart, nextEnd) = setSeriesTimes(entry.start, entry.end)
        entry.copy(
          module = s.module,
          courseType = s.courseType,
          rooms = s.rooms,
          lecturer = s.lecturer,
          start = nextStart,
          end = nextEnd,
          po = s.po,
        )
      }
      counts <- DBIO.sequence(entries.zip(updated).map {
        case (oldEntry, newEntry) =>
          tableQuery
            .filter(_.id === oldEntry.id)
            .map(e => (e.module, e.courseType, e.rooms, e.lecturer, e.start, e.end, e.po))
            .update(
              (
                newEntry.module,
                newEntry.courseType,
                newEntry.rooms,
                newEntry.lecturer,
                newEntry.start,
                newEntry.end,
                newEntry.po,
              )
            )
      })
      _ <-
        if counts.sum == updated.size then DBIO.unit
        else DBIO.failed(new IllegalStateException("not all schedule entries in series were updated"))
      xs <- sql"select schedule.get_schedule_entries(${updated.map(_.id).toList})".as[String].head
    } yield xs

    db.run(query.transactionally)
  }

  /**
   * Checks whether a schedule entry series exists for `seriesId` and returns its entry times.
   * A series must contain at least two entries; a single matching entry is treated as no series.
   *
   * @param seriesId the series identifier to look up
   * @return id/start/end pairs for every entry in the series, or an empty sequence if no series exists
   */
  def hasSeries(seriesId: ScheduleEntrySeriesId): Future[Seq[SeriesOccurrence]] =
    db.run(tableQuery.filter(_.seriesId === seriesId).map(a => (a.id, a.start, a.end)).result).map {
      case series if series.size <= 1 => Seq.empty
      case series                     => series.map((i, s, e) => SeriesOccurrence(i, s, e))
    }

  /**
   * Deletes the schedule entry with the given ID.
   *
   * @param id the ID of the schedule entry to delete
   */
  def delete(id: UUID): Future[Boolean] =
    db.run(tableQuery.filter(_.id === id).delete).map(_ == 1)

  /** Explicit creation sets sourcePlanDraft and sourceScheduleEntryDraft to None. */
  private def toDbEntry(p: ScheduleEntryProtocol): ScheduleEntryDbEntry =
    ScheduleEntryDbEntry(
      UUID.randomUUID(),
      p.seriesId,
      p.module,
      p.courseType,
      p.rooms,
      p.lecturer,
      p.start,
      p.end,
      p.po,
      None,
      None
    )
}

object ScheduleEntryRepository {
  private val ScheduleZone = ZoneId.of("Europe/Berlin")

  /**
   * Builds the time update used by `updateSeries`.
   *
   * `newStart` and `newEnd` come from the edited entry and stay constant for the whole series. Each call keeps the
   * local start and end dates from one series entry and sets its local start and end times to the new times.
   */
  private[database] def setSeriesTimes(newStart: Instant, newEnd: Instant)(
      seriesStart: Instant,
      seriesEnd: Instant
  ): (Instant, Instant) = {
    val newStartTime   = newStart.atZone(ScheduleZone).toLocalTime
    val newEndTime     = newEnd.atZone(ScheduleZone).toLocalTime
    val seriesStartDay = seriesStart.atZone(ScheduleZone).toLocalDate
    val seriesEndDay   = seriesEnd.atZone(ScheduleZone).toLocalDate

    val nextStart = seriesStartDay.atTime(newStartTime).atZone(ScheduleZone).toInstant
    val nextEnd   = seriesEndDay.atTime(newEndTime).atZone(ScheduleZone).toInstant

    (nextStart, nextEnd)
  }
}
