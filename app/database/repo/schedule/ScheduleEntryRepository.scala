package database.repo.schedule

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.schedule.ScheduleEntryTable
import models.schedule.ScheduleEntry
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import models.Semester
import database.Schema

@Singleton
final class ScheduleEntryRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
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
   *
   * @param entries list of schedule entries to insert
   * @return JSON string of the created schedule entries
   */
  def create(entries: List[ScheduleEntry.DB]): Future[String] = {
    import database.MyPostgresProfile.MyAPI.setUUIDArray

    val query = for {
      _  <- tableQuery ++= entries
      xs <- sql"select schedule.get_schedule_entries(${entries.map(_.id)})".as[String].head
    } yield xs
    db.run(query.transactionally)
  }

  /**
   * Updates an existing schedule entry and returns it as a JSON string.
   *
   * @param s the schedule entry with updated values
   * @return JSON string of the updated schedule entry
   */
  def update(s: ScheduleEntry.DB): Future[String] = {
    import database.MyPostgresProfile.MyAPI.setUUIDArray

    val query = for {
      _  <- tableQuery.filter(_.id === s.id).update(s)
      xs <- sql"select schedule.get_schedule_entries(${Seq(s.id)})".as[String].head
    } yield xs
    db.run(query.transactionally)
  }

  /**
   * Deletes the schedule entry with the given ID.
   *
   * @param id the ID of the schedule entry to delete
   */
  def delete(id: UUID): Future[Unit] =
    db.run(tableQuery.filter(_.id === id).delete).map(_ => ())

  /**
   * Creates the next semester's partition if it does not already exist.
   *
   * @return true if the partition was created, false if it already existed
   */
  def createNextPartitionIfNotExists(): Future[Boolean] = {
    val semesterId    = Semester.next(LocalDate.now()).id
    val (start, end)  = Semester.dateRange(semesterId)
    val partitionName = s"schedule_entry_${semesterId}"
    val schema        = Schema.Schedule.name

    val query = for {
      exists <- sql"""
        SELECT EXISTS (
          SELECT 1 FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
          WHERE n.nspname = $schema AND c.relname = $partitionName
        )
        """.as[Boolean].head
      res <-
        if exists then DBIO.successful(false)
        else {
          val zone     = ZoneId.of("Europe/Berlin")
          val pattern  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxx")
          val startStr = start.atZone(zone).format(pattern)
          val endStr   = end.atZone(zone).format(pattern)
          sqlu"""
          CREATE TABLE IF NOT EXISTS #$schema.#$partitionName
            PARTITION OF #$schema.schedule_entry
            FOR VALUES FROM ('#$startStr') TO ('#$endStr')
        """.map(_ => true)
        }
    } yield res

    db.run(query)
  }
}
