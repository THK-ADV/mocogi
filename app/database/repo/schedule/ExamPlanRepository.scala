package database.repo.schedule

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.core.AssessmentMethodTable
import database.table.core.IdentityTable
import database.table.schedule.*
import models.schedule.*
import models.Semester
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import slick.jdbc.GetResult
import slick.jdbc.JdbcProfile

/**
 * Exam plan drafts, their appointments and the published copies. Writes lock the plan draft row, which serializes
 * them with publication. Unlike schedule plans, exam plans stay editable after publication.
 */
@Singleton
final class ExamPlanRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.given_BaseColumnType_PlanDraftKind
  import database.MyPostgresProfile.MyAPI.getJson
  import database.MyPostgresProfile.MyAPI.setJson
  import database.MyPostgresProfile.MyAPI.setStringArray
  import database.MyPostgresProfile.MyAPI.setUUIDArray
  import database.MyPostgresProfile.MyAPI.setUUIDArrayOption
  import database.MyPostgresProfile.MyAPI.PgArrayPositionedResult
  import profile.api.*

  private val planDrafts  = TableQuery[PlanDraftTable]
  private val entryDrafts = TableQuery[ExamEntryDraftTable]

  /** Exam plan drafts, optionally only the one of the semester. */
  def all(semester: Option[String]): Future[Seq[PlanDraft]] =
    db.run(planDrafts.filter(_.kind === PlanDraftKind.Exam).filterOpt(semester)(_.semester === _).result)

  /** The exam plan draft with its semester and whether it differs from its published copies. */
  def get(id: UUID): Future[Option[(PlanDraft, Semester, Boolean)]] =
    db.run(
      planDrafts.filter(d => d.id === id && d.kind === PlanDraftKind.Exam).result.headOption.flatMap {
        case Some(plan) => hasUnpublishedChanges(id).map(c => Some((plan, Semester(plan.semester), c)))
        case None       => DBIO.successful(None)
      }
    )

  /** Creates the exam plan draft of the semester, failing if one already exists. */
  def create(p: PlanDraftProtocol): Future[Unit] = {
    val now = LocalDateTime.now()
    run(require(p.semester.matches("(sose|wise)_\\d{4}"), s"invalid semester ${p.semester}")) {
      for {
        exists <- planDrafts.filter(d => d.kind === PlanDraftKind.Exam && d.semester === p.semester).exists.result
        _      <-
          if exists then DBIO.failed(new IllegalArgumentException("exam plan already exists for semester"))
          else planDrafts += PlanDraft(UUID.randomUUID(), PlanDraftKind.Exam, p.semester, now, now, None)
      } yield ()
    }
  }

  /** Deletes the exam plan draft with its appointments and returns `false` if it was ever published. */
  def delete(id: UUID): Future[Boolean] = {
    val query = examPlan(id, lock = true).flatMap(plan =>
      if plan.publishedAt.nonEmpty then DBIO.successful(false)
      else planDrafts.filter(_.id === id).delete.map(_ => true)
    )
    db.run(query.transactionally)
  }

  /** Planning pool selected by the configuration, see `exams.exam_pool`. */
  def pool(c: ExamPoolConfig): Future[JsValue] =
    run(ExamPoolConfig.validate(c)) {
      knownMethods(c) >>
        sql"""SELECT exams.exam_pool(${c.semester}, ${c.phases}::text[], ${c.assessmentMethods}::text[])"""
          .as[JsValue]
          .head
    }

  /** Shared pool configuration of the exam plan draft, the default one if none is stored. */
  def poolConfig(planDraft: UUID): Future[ExamPoolConfig] =
    db.run(examPlan(planDraft, lock = false).flatMap(storedConfig))

  /**
   * Stores the pool configuration of the exam plan draft if it changes. The semester must be the plan's one and the
   * available invigilators persons. Existing invigilators are kept.
   */
  def setPoolConfig(planDraft: UUID, c: ExamPoolConfig): Future[ExamPoolConfig] =
    run(ExamPoolConfig.validate(c)) {
      for {
        plan <- examPlan(planDraft, lock = true)
        _    <-
          if c.semester == plan.semester then DBIO.unit
          else DBIO.failed(new IllegalArgumentException(s"semester must be ${plan.semester}"))
        _       <- knownMethods(c)
        _       <- persons("available invigilators must be persons", c.availableInvigilators)
        current <- storedConfig(plan)
        _       <-
          if current == c then DBIO.successful(0)
          else sqlu"""INSERT INTO schedule.exam_pool_config (plan_draft, config)
                    VALUES (${planDraft.toString}::uuid, ${Json.toJson(c)})
                    ON CONFLICT (plan_draft) DO UPDATE SET config = excluded.config"""
      } yield c
    }

  /** Appointments of the exam plan draft, optionally only those overlapping the interval. */
  def drafts(planDraft: UUID, interval: Option[(Instant, Instant)]): Future[JsValue] =
    db.run(examPlan(planDraft, lock = false) >> entries(Some(planDraft), None, None, interval))

  /** Published appointments of the semester and/or overlapping the interval, without the planning figures. */
  def published(semester: Option[String], interval: Option[(Instant, Instant)]): Future[JsValue] =
    db.run(entries(None, semester, None, interval))

  /**
   * Creates appointments of existing exams. The exams are locked against merges and deletions, which would otherwise
   * fail the insert on the foreign key.
   *
   * @return the created appointments
   */
  def createEntries(planDraft: UUID, requests: List[ExamEntryCreate]): Future[JsValue] = {
    val ids   = requests.map(_ => UUID.randomUUID())
    val exams = requests.map(_.exam).distinct
    run {
      require(requests.nonEmpty, "at least one exam entry is required")
      requests.foreach(ExamEntryCreate.validate)
    } {
      for {
        _     <- examPlan(planDraft, lock = true)
        known <- sql"SELECT id::text FROM exams.exam WHERE id = ANY ($exams) ORDER BY id FOR KEY SHARE"
          .as[String]
          .map(_.map(UUID.fromString))
        _ <- missing("unknown exams", exams.filterNot(known.toSet))
        _ <- knownAssessmentMethods(requests.map(_.assessmentMethod))
        _ <- entryDrafts ++= ids
          .zip(requests)
          .map((id, r) =>
            (id, planDraft, r.exam, r.phase, r.start, r.end, r.expectedParticipants, Some(r.assessmentMethod))
          )
        _    <- touch(planDraft)
        json <- entries(Some(planDraft), None, Some(ids), None)
      } yield json
    }
  }

  /** Replaces times, expected participants and assessment method if they change, keeping the rooms. */
  def updateEntry(planDraft: UUID, entry: UUID, u: ExamEntryUpdate): Future[JsValue] = {
    val fields = entryOf(planDraft, entry).map(e => (e.start, e.end, e.expectedParticipants, e.assessmentMethod))
    val values = (u.start, u.end, u.expectedParticipants, u.assessmentMethod)
    run(ExamEntryUpdate.validate(u.start, u.end, u.expectedParticipants)) {
      for {
        _       <- examPlan(planDraft, lock = true)
        current <- fields.result.headOption.flatMap {
          case Some(current) => DBIO.successful(current)
          case None          => DBIO.failed(new NoSuchElementException("exam entry not found"))
        }
        _ <-
          if u.assessmentMethod.isDefined || current._4.isEmpty then DBIO.unit
          else DBIO.failed(new IllegalArgumentException("assessmentMethod must not be cleared"))
        _    <- knownAssessmentMethods(u.assessmentMethod.toList)
        _    <- if current == values then DBIO.unit else fields.update(values) >> touch(planDraft)
        json <- single(planDraft, entry)
      } yield json
    }
  }

  /**
   * Replaces the room allocations of the appointment. Invigilators are stored as a sorted set of person ids, which must
   * not exceed a known `requiredInvigilators`. The partition is trimmed and must be given completely or not at all.
   */
  def setRooms(planDraft: UUID, entry: UUID, rooms: List[ExamRoomAllocation]): Future[JsValue] = {
    val allocations = rooms.map(r =>
      r.copy(
        invigilators = r.invigilators.distinct.sorted,
        partitionBy = r.partitionBy.map(_.trim),
        partitionValue = r.partitionValue.map(_.trim)
      )
    )
    val roomIds      = rooms.map(_.room)
    val invigilators = allocations.flatMap(_.invigilators).distinct
    run {
      require(roomIds.distinct.size == roomIds.size, "a room must be allocated only once")
      require(rooms.forall(_.allocatedSeats.forall(_ >= 0)), "allocatedSeats must not be negative")
      require(rooms.forall(_.requiredInvigilators.forall(_ >= 0)), "requiredInvigilators must not be negative")
      allocations.foreach(a =>
        a.requiredInvigilators.foreach(n =>
          require(
            a.invigilators.size <= n,
            s"room ${a.room} has ${a.invigilators.size} invigilators, but requires only $n"
          )
        )
      )
      require(
        allocations.forall(a =>
          a.partitionBy.isDefined == a.partitionValue.isDefined && (a.partitionBy ++ a.partitionValue)
            .forall(_.nonEmpty)
        ),
        "partitionBy and partitionValue must both be nonblank or both null"
      )
    } {
      for {
        _       <- examPlan(planDraft, lock = true)
        exists  <- entryOf(planDraft, entry).exists.result
        _       <- if exists then DBIO.successful(()) else DBIO.failed(new NoSuchElementException("exam entry not found"))
        known   <- TableQuery[RoomTable].filter(_.id.inSet(roomIds)).map(_.id).result
        _       <- missing("unknown rooms", roomIds.filterNot(known.toSet))
        _       <- persons("invigilators must be persons", invigilators)
        changed <- sql"SELECT schedule.set_exam_room_drafts(${entry.toString}::uuid, ${Json.toJson(allocations)})"
          .as[Boolean]
          .head
        _    <- if changed then touch(planDraft) else DBIO.unit
        json <- single(planDraft, entry)
      } yield json
    }
  }

  /**
   * Fills the open known demand of the selected rooms of the exam plan draft with active persons of the available
   * invigilators of its pool configuration, see [[InvigilationDuty.assign]]. Writes only the rooms that change.
   *
   * @return
   *   all appointments of the plan
   */
  def assignInvigilators(planDraft: UUID, selected: Set[InvigilationDutyRef]): Future[JsValue] = {
    val query = for {
      plan       <- examPlan(planDraft, lock = true)
      config     <- storedConfig(plan)
      candidates <- TableQuery[IdentityTable]
        .filter(i => i.id.inSet(config.availableInvigilators) && i.isPerson && i.isActive)
        .sortBy(_.id)
        .map(_.id)
        .result
      duties <-
        if candidates.isEmpty then DBIO.successful(Vector.empty)
        else
          sql"""SELECT draft.id::text, draft_room.room::text, draft."start", draft."end", draft_room.required_invigilators, draft_room.invigilators
                   FROM schedule.exam_entry_draft draft
                   JOIN schedule.exam_room_draft draft_room ON draft_room.exam_entry_draft = draft.id
                   WHERE draft.plan_draft = ${planDraft.toString}::uuid
                   ORDER BY draft."start", draft.id, draft_room.room""".as[InvigilationDuty]
      changed = InvigilationDuty.assign(duties, candidates, selected)
      _ <-
        if changed.isEmpty then DBIO.unit
        else {
          val assignments =
            Json.toJson(
              changed.map(d => Json.obj("entry" -> d.entry, "room" -> d.room, "invigilators" -> d.invigilators))
            )
          sqlu"""UPDATE schedule.exam_room_draft draft_room
                 SET invigilators = assignment.invigilators
                 FROM jsonb_to_recordset($assignments) AS assignment(entry uuid, room uuid, invigilators text[])
                 WHERE draft_room.exam_entry_draft = assignment.entry AND draft_room.room = assignment.room""" >>
            touch(planDraft)
        }
      json <- entries(Some(planDraft), None, None, None)
    } yield json
    db.run(query.transactionally)
  }

  /** Deletes the appointment with its rooms and returns `false` if it does not exist. */
  def deleteEntry(planDraft: UUID, entry: UUID): Future[Boolean] = {
    val query = for {
      _     <- examPlan(planDraft, lock = true)
      count <- entryOf(planDraft, entry).delete
      _     <- if count == 1 then touch(planDraft) else DBIO.successful(0)
    } yield count == 1
    db.run(query.transactionally)
  }

  /** Publishes the exam plan draft, see `schedule.publish_exam_plan`. An empty draft removes its published copies. */
  def publish(planDraft: UUID): Future[Unit] = {
    val query = examPlan(planDraft, lock = true) >>
      sql"SELECT schedule.publish_exam_plan(${planDraft.toString}::uuid)".as[Int].head.map(_ => ())
    db.run(query.transactionally)
  }

  /** Compares the contents of the exam plan draft with its published copies. */
  private def hasUnpublishedChanges(planDraft: UUID): DBIO[Boolean] =
    sql"SELECT schedule.exam_plan_has_unpublished_changes(${planDraft.toString}::uuid)".as[Boolean].head

  /** Runs the validation and then the action in one transaction, failing the future if either throws. */
  private def run[A](validate: => Unit)(action: DBIO[A]): Future[A] =
    Future(validate).flatMap(_ => db.run(action.transactionally))

  private def entryOf(planDraft: UUID, entry: UUID) =
    entryDrafts.filter(e => e.id === entry && e.planDraft === planDraft)

  private def storedConfig(plan: PlanDraft): DBIO[ExamPoolConfig] =
    sql"SELECT config FROM schedule.exam_pool_config WHERE plan_draft = ${plan.id.toString}::uuid"
      .as[JsValue]
      .headOption
      .map(_.fold(ExamPoolConfig.default(plan.semester))(_.as[ExamPoolConfig]))

  private def knownMethods(c: ExamPoolConfig): DBIO[Unit] = knownAssessmentMethods(c.assessmentMethods)

  private def knownAssessmentMethods(methods: List[String]): DBIO[Unit] =
    TableQuery[AssessmentMethodTable]
      .filter(_.id.inSet(methods))
      .map(_.id)
      .result
      .flatMap(known => missing("unknown assessment methods", methods.filterNot(known.toSet)))

  private def persons(message: String, ids: List[String]): DBIO[Unit] =
    TableQuery[IdentityTable]
      .filter(i => i.id.inSet(ids) && i.isPerson)
      .map(_.id)
      .result
      .flatMap(known => missing(message, ids.filterNot(known.toSet)))

  private given GetResult[InvigilationDuty] =
    GetResult(r =>
      InvigilationDuty(
        UUID.fromString(r.nextString()),
        UUID.fromString(r.nextString()),
        r.nextTimestamp().toInstant,
        r.nextTimestamp().toInstant,
        r.nextIntOption(),
        r.nextArray[String]().toList
      )
    )

  private def missing(message: String, ids: Seq[Any]): DBIO[Unit] =
    if ids.isEmpty then DBIO.successful(())
    else DBIO.failed(new IllegalArgumentException(s"$message: ${ids.mkString(", ")}"))

  /** Returns the exam plan draft, locking its row if requested. */
  private def examPlan(id: UUID, lock: Boolean): DBIO[PlanDraft] = {
    val query = planDrafts.filter(_.id === id)
    (if lock then query.forUpdate else query).result.headOption.flatMap {
      case Some(plan) if plan.kind == PlanDraftKind.Exam => DBIO.successful(plan)
      case Some(_)                                       => DBIO.failed(new IllegalArgumentException("plan draft is not an exam plan"))
      case None                                          => DBIO.failed(new NoSuchElementException("plan draft not found"))
    }
  }

  private def touch(planDraft: UUID): DBIO[Int] =
    planDrafts.filter(_.id === planDraft).map(_.updatedAt).update(LocalDateTime.now())

  /** Appointments matching all defined filters, see `schedule.get_exam_entries`. */
  private def entries(
      planDraft: Option[UUID],
      semester: Option[String],
      ids: Option[List[UUID]],
      interval: Option[(Instant, Instant)]
  ): DBIO[JsValue] = {
    val from = interval.map(i => Timestamp.from(i._1))
    val to   = interval.map(i => Timestamp.from(i._2))
    sql"""SELECT schedule.get_exam_entries(${planDraft.map(_.toString)}::uuid, $semester, $ids::uuid[], $from::timestamptz, $to::timestamptz)"""
      .as[JsValue]
      .head
  }

  private def single(planDraft: UUID, entry: UUID): DBIO[JsValue] =
    entries(Some(planDraft), None, Some(List(entry)), None).map(json => (json \ 0).get)
}
