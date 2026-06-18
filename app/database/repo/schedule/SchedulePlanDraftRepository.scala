package database.repo.schedule

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import database.table.schedule.PlanDraftTable
import database.table.schedule.ScheduleEntryDraftTable
import database.table.schedule.ScheduleEntryTable
import models.schedule.PlanDraft
import models.schedule.PlanDraftKind
import models.schedule.PlanDraftProtocol
import models.schedule.ScheduleEntry
import models.schedule.ScheduleEntryDraft
import models.Semester
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.Json
import slick.jdbc.JdbcProfile

@Singleton
final class SchedulePlanDraftRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*
  import database.table.given_BaseColumnType_PlanDraftKind

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
   * Returns all schedule entry drafts for the given plan draft.
   * Throws an exception if the plan draft id is not a schedule draft.
   */
  def scheduleEntriesDrafts(planDraft: UUID): Future[String] = {
    val query = for {
      _  <- ensureScheduleDraft(planDraft)
      xs <- sql"select schedule.get_schedule_entry_drafts(${planDraft.toString}::uuid)".as[String].head
    } yield xs
    db.run(query)
  }

  /**
   * Creates new schedule entry drafts for the given plan draft and updates the plan draft's updatedAt column.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def createScheduleEntriesDrafts(planDraft: UUID, entries: List[ScheduleEntryDraft.JSON]): Future[Unit] =
    if (entries.exists(_.planDraft != planDraft)) {
      Future.failed(new IllegalArgumentException("plan draft id does not match the plan draft id in the payload"))
    } else {
      val query = for {
        _ <- ensureActiveScheduleDraft(planDraft)
        _ <- scheduleDrafts ++= entries.map(_.copy(id = UUID.randomUUID(), planDraft = planDraft))
        _ <- touch(planDraft)
      } yield ()
      db.run(query.transactionally)
    }

  /**
   * Updates an existing schedule entry draft for the given entry id and updates the plan draft's updatedAt column.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def updateScheduleEntryDraft(entryId: UUID, payload: ScheduleEntryDraft.JSON): Future[Unit] = {
    val query = for {
      _     <- ensureActiveScheduleDraft(payload.planDraft)
      count <- scheduleDrafts
        .filter(e => e.planDraft === payload.planDraft && e.id === entryId)
        .update(payload.copy(id = entryId))
      _ <- requireUpdated(count, "schedule entry draft not found")
      _ <- touch(payload.planDraft)
    } yield ()
    db.run(query.transactionally)
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
    def createLiveEntries(planDraft: UUID, entries: Seq[ScheduleEntryDraft.DB]) = {
      val liveEntries = entries.toList.map(draft =>
        ScheduleEntry(
          UUID.randomUUID(),
          draft.seriesId,
          draft.module,
          draft.courseType,
          draft.rooms,
          draft.start,
          draft.end,
          Json.obj("po" -> draft.po, "lecturer" -> draft.lecturer),
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

    def getDraftEntries(planDraft: UUID): DBIO[Seq[ScheduleEntryDraft.DB]] =
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
}
