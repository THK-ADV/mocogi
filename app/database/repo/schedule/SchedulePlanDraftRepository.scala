package database.repo.schedule

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import database.table.schedule.*
import models.schedule.*
import models.Semester
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class SchedulePlanDraftRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.given_BaseColumnType_CourseType
  import database.table.given_BaseColumnType_PlanDraftKind
  import database.table.scheduleEntrySeriesIdColumnType
  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.setUUIDArray
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper
  import profile.api.*

  private val planDrafts         = TableQuery[PlanDraftTable]
  private val scheduleDrafts     = TableQuery[ScheduleEntryDraftTable]
  private val scheduleEntryTable = TableQuery[ScheduleEntryTable]

  def all(kind: PlanDraftKind, semester: Option[String], activeOnly: Boolean): Future[Seq[PlanDraft]] =
    db.run(
      planDrafts.filter { d =>
        def semesterMatch: Rep[Boolean] = semester.map(s => d.semester === s).getOrElse(true)
        def activeMatch: Rep[Boolean]   = if activeOnly then d.publishedAt.isEmpty else true
        d.kind === kind && semesterMatch && activeMatch
      }.result
    )

  def get(id: UUID, kind: PlanDraftKind): Future[Option[(PlanDraft, Semester)]] =
    db.run(
      planDrafts
        .filter(d => d.id === id && d.kind === kind)
        .result
        .headOption
        .map(_.map(d => (d, Semester.apply(d.semester))))
    )

  def create(p: PlanDraftProtocol): Future[Unit] = {
    val semesterId =
      try Semester.apply(p.semester).id
      catch case NonFatal(_) => throw new IllegalArgumentException(s"invalid semester: ${p.semester}")
    val now   = LocalDateTime.now()
    val draft = PlanDraft(UUID.randomUUID(), p.kind, semesterId, now, now, None)
    db.run(planDrafts += draft).map(_ => ())
  }

  def deleteActive(id: UUID): Future[Unit] =
    db.run(planDrafts.filter(d => d.id === id && d.publishedAt.isEmpty).delete.map(_ => ()))

  /**
   * Returns all schedule entry drafts for the given plan draft within the given time range.
   * Throws an exception if the plan draft id is not a schedule draft.
   */
  def scheduleEntriesDrafts(planDraft: UUID, from: Timestamp, to: Timestamp): Future[String] = {
    val query = for {
      _  <- ensureScheduleDraft(planDraft)
      xs <- sql"select schedule.get_schedule_entry_drafts(${planDraft.toString}::uuid, $from, $to)".as[String].head
    } yield xs
    db.run(query)
  }

  /**
   * Creates new schedule entry drafts for the given plan draft and updates the plan draft's updatedAt column.
   * Throws an exception if the plan draft id is not an active schedule draft.
   *
   * @return JSON string of the created schedule entry drafts
   */
  def createScheduleEntriesDrafts(planDraft: UUID, entries: List[ScheduleEntryProtocol]): Future[String] = {
    val dbEntries = entries.map(toDbEntry(_, planDraft))
    val query     = for {
      _  <- ensureActiveScheduleDraft(planDraft)
      _  <- scheduleDrafts ++= dbEntries
      _  <- touch(planDraft)
      xs <- sql"select schedule.get_schedule_entry_drafts(${dbEntries.map(_.id)})".as[String].head
    } yield xs
    db.run(query.transactionally)
  }

  /**
   * Updates an existing schedule entry draft for the given entry id and updates the plan draft's updatedAt column.
   * All properties except seriesId and planDraftId are updatable.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def updateScheduleEntryDraft(planDraftId: UUID, entryId: UUID, p: ScheduleEntryProtocol): Future[String] = {
    val query = for {
      _     <- ensureActiveScheduleDraft(planDraftId)
      count <- scheduleDrafts
        .filter(e => e.planDraft === planDraftId && e.id === entryId)
        .map(e => (e.module, e.courseType, e.rooms, e.lecturer, e.start, e.end, e.po))
        .update((p.module, p.courseType, p.rooms, p.lecturer, p.start, p.end, p.po))
      _  <- requireUpdated(count, "failed to update schedule entry draft")
      _  <- touch(planDraftId)
      xs <- sql"select schedule.get_schedule_entry_drafts(${List(entryId)})".as[String].head
    } yield xs
    db.run(query.transactionally)
  }

  /**
   * Updates all schedule entry drafts in the same series as `entryId` for the given plan draft.
   * The edited anchor entry defines the local start and end times for every draft in the series.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def updateScheduleEntryDraftSeries(planDraftId: UUID, entryId: UUID, p: ScheduleEntryProtocol): Future[String] = {
    val query = for {
      _      <- ensureActiveScheduleDraft(planDraftId)
      anchor <- scheduleDrafts
        .filter(e => e.planDraft === planDraftId && e.id === entryId)
        .forUpdate
        .result
        .headOption
        .flatMap {
          case Some(entry) => DBIO.successful(entry)
          case None        => DBIO.failed(new NoSuchElementException("schedule entry draft not found"))
        }
      _ <-
        if anchor.seriesId == p.seriesId then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("seriesId does not match anchor entry"))
      _ <-
        if p.end.isAfter(p.start) then DBIO.unit
        else DBIO.failed(new IllegalArgumentException("end must be after start"))
      entries <- scheduleDrafts
        .filter(e => e.planDraft === planDraftId && e.seriesId === anchor.seriesId)
        .forUpdate
        .result
      setSeriesTimes = ScheduleEntryRepository.setSeriesTimes(p.start, p.end)
      updated        = entries.map { entry =>
        val (nextStart, nextEnd) = setSeriesTimes(entry.start, entry.end)
        entry.copy(
          module = p.module,
          courseType = p.courseType,
          rooms = p.rooms,
          lecturer = p.lecturer,
          start = nextStart,
          end = nextEnd,
          po = p.po,
        )
      }
      counts <- DBIO.sequence(entries.zip(updated).map {
        case (oldEntry, newEntry) =>
          scheduleDrafts
            .filter(e => e.planDraft === planDraftId && e.id === oldEntry.id)
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
        else DBIO.failed(new IllegalStateException("not all schedule entry drafts in series were updated"))
      _  <- touch(planDraftId)
      xs <- sql"select schedule.get_schedule_entry_drafts(${updated.map(_.id).toList})".as[String].head
    } yield xs

    db.run(query.transactionally)
  }

  /**
   * Checks whether a schedule entry draft series exists for `seriesId` in `planDraftId` and returns its entry times.
   * A series must contain at least two entries; a single matching entry is treated as no series.
   *
   * @param planDraftId the schedule plan draft identifier to look up
   * @param seriesId the series identifier to look up
   * @return id/start/end pairs for every entry in the series, or an empty sequence if no series exists
   */
  def hasSeries(planDraftId: UUID, seriesId: ScheduleEntrySeriesId): Future[Seq[SeriesOccurrence]] = {
    val query = for {
      _      <- ensureScheduleDraft(planDraftId)
      series <- scheduleDrafts
        .filter(e => e.planDraft === planDraftId && e.seriesId === seriesId)
        .map(e => (e.id, e.start, e.end))
        .result
    } yield series match {
      case series if series.size <= 1 => Seq.empty
      case series                     => series.map((i, s, e) => SeriesOccurrence(i, s, e))
    }
    db.run(query)
  }

  /**
   * Deletes an existing schedule entry draft for the given plan draft and entry id.
   * Throws an exception if the plan draft id is not an active schedule draft or the entry id is not a schedule entry draft.
   */
  def deleteScheduleEntryDraft(planDraft: UUID, entryId: UUID): Future[Boolean] = {
    val query = for {
      _     <- ensureActiveScheduleDraft(planDraft)
      count <- scheduleDrafts.filter(e => e.planDraft === planDraft && e.id === entryId).delete
      _     <- if count == 1 then touch(planDraft) else DBIO.successful(0)
    } yield count == 1
    db.run(query.transactionally)
  }

  /**
   * Publishes an active schedule plan draft: copies every entry draft into a live schedule entry
   * and sets publishedAt on the plan draft. Runs in a single transaction.
   * Throws if the plan draft is missing, not a schedule draft, already published, or has no entry drafts.
   */
  def publish(planDraft: UUID): Future[Unit] = {
    def createLiveEntries(planDraft: UUID, entries: Seq[ScheduleEntryDraftDbEntry]) = {
      val liveEntries = entries.toList.map(draft =>
        ScheduleEntryDbEntry(
          UUID.randomUUID(),
          draft.seriesId,
          draft.module,
          draft.courseType,
          draft.rooms,
          draft.lecturer,
          draft.start,
          draft.end,
          draft.po,
          Some(planDraft),
          Some(draft.id),
        )
      )
      scheduleEntryTable ++= liveEntries
    }

    def archivePlanDraft(planDraft: UUID): DBIO[Int] = {
      val now = LocalDateTime.now()
      planDrafts
        .filter(d => d.id === planDraft && d.publishedAt.isEmpty)
        .map(d => (d.updatedAt, d.publishedAt))
        .update((now, Some(now)))
    }

    def getDraftEntries(planDraft: UUID): DBIO[Seq[ScheduleEntryDraftDbEntry]] =
      for {
        entries <- scheduleDrafts.filter(_.planDraft === planDraft).result
        _       <-
          if (entries.isEmpty) DBIO.failed(new IllegalArgumentException("cannot publish an empty schedule plan draft"))
          else DBIO.unit
      } yield entries

    val query = for {
      _       <- ensureActiveScheduleDraft(planDraft)
      entries <- getDraftEntries(planDraft)
      _       <- createLiveEntries(planDraft, entries)
      count   <- archivePlanDraft(planDraft)
      _       <- requireUpdated(count, "schedule plan draft was already published")
    } yield ()
    db.run(query.transactionally)
  }

  /**
   * Ensures that the plan draft is a schedule draft.
   */
  private def ensureScheduleDraft(planDraft: UUID): DBIO[Unit] =
    planDrafts.filter(_.id === planDraft).result.headOption.flatMap {
      case Some(draft) if draft.kind != PlanDraftKind.Schedule =>
        DBIO.failed(new IllegalArgumentException("plan draft is not a schedule draft"))
      case Some(_) => DBIO.unit
      case None    => DBIO.failed(new IllegalArgumentException("plan draft not found"))
    }

  /**
   * Ensures that the plan draft is an active schedule draft.
   */
  private def ensureActiveScheduleDraft(planDraft: UUID): DBIO[Unit] =
    planDrafts.filter(_.id === planDraft).forUpdate.result.headOption.flatMap {
      case Some(draft) if draft.kind != PlanDraftKind.Schedule =>
        DBIO.failed(new IllegalArgumentException("plan draft is not a schedule draft"))
      case Some(draft) if draft.publishedAt.nonEmpty =>
        DBIO.failed(new IllegalArgumentException("plan draft is already published"))
      case Some(_) =>
        DBIO.unit
      case None =>
        DBIO.failed(new IllegalArgumentException("plan draft not found"))
    }

  /**
   * Updates the updatedAt column of the plan draft to the current time.
   */
  private def touch(planDraft: UUID): DBIO[Int] =
    planDrafts.filter(_.id === planDraft).map(_.updatedAt).update(LocalDateTime.now())

  private def requireUpdated(count: Int, message: String): DBIO[Unit] =
    if count == 1 then DBIO.unit
    else DBIO.failed(new IllegalArgumentException(message))

  private def toDbEntry(d: ScheduleEntryProtocol, planDraft: UUID) =
    ScheduleEntryDraftDbEntry(
      UUID.randomUUID(),
      planDraft,
      d.seriesId,
      d.module,
      d.courseType,
      d.rooms,
      d.lecturer,
      d.start,
      d.end,
      d.po
    )
}
