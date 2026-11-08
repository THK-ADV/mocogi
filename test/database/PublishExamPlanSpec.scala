package database

import java.util.UUID

import database.MyPostgresProfile.api.*

/** Integration tests of `schedule.publish_exam_plan` and `schedule.exam_plan_has_unpublished_changes`. */
final class PublishExamPlanSpec extends ExamFixtures {

  protected def idPrefix = 4

  private val plan      = id(1)
  private val otherPlan = id(2)
  private val math      = id(101)
  private val physics   = id(102)
  private val hall      = id(201)
  private val lab       = id(202)
  private val annex     = id(203)
  private val moved     = id(11) // moved and its rooms changed after the first publication
  private val removed   = id(12) // deleted after the first publication
  private val kept      = id(13) // unchanged after the first publication
  private val added     = id(14) // created after the first publication
  private val foreign   = id(21) // of the other plan

  private val fixtures = DBIO.seq(
    examPlan(plan, "wise_2098"),
    examPlan(otherPlan, "sose_2098"),
    exam(math, "Mathe"),
    exam(physics, "Physik"),
    insertRoom(hall, "Fixture Hall"),
    insertRoom(lab, "Fixture Lab"),
    insertRoom(annex, "Fixture Annex"),
    entryDraft(moved, plan, math, 9, 11, Some("written-exam")),
    roomDraft(moved, hall, List("aaz", "abe"), Some(("Nachname", "A-K"))),
    roomDraft(moved, lab),
    entryDraft(removed, plan, physics, 12, 13),
    roomDraft(removed, hall, List("abr")),
    entryDraft(kept, plan, physics, 14, 15, Some("oral-exam"), "wise_2"),
    roomDraft(kept, lab, List("aaz")),
    entryDraft(foreign, otherPlan, math, 9, 10),
    roomDraft(foreign, hall, List("abe"))
  )

  private type Entry = (String, String, String, String, Option[String], String, String)
  private type Room  = (String, String, String, Option[String], Option[String])

  /** Published fields of the drafts of the plan keyed by draft id. */
  private def drafts(p: UUID): DBIO[(Vector[Entry], Vector[Room])] =
    sql"""SELECT draft.id::text, plan.semester, draft.exam::text, draft.phase, draft.assessment_method,
                 draft."start"::text, draft."end"::text
          FROM schedule.exam_entry_draft draft JOIN schedule.plan_draft plan ON plan.id = draft.plan_draft
          WHERE draft.plan_draft = ${p.toString}::uuid ORDER BY 1"""
      .as[Entry]
      .zip(sql"""SELECT draft_room.exam_entry_draft::text, draft_room.room::text, draft_room.invigilators::text,
                   draft_room.partition_by, draft_room.partition_value
            FROM schedule.exam_room_draft draft_room
            JOIN schedule.exam_entry_draft draft ON draft.id = draft_room.exam_entry_draft
            WHERE draft.plan_draft = ${p.toString}::uuid ORDER BY 1, 2""".as[Room])

  /** Published copies of the plan keyed by their source draft id, in the shape of [[drafts]]. */
  private def published(p: UUID): DBIO[(Vector[Entry], Vector[Room])] =
    sql"""SELECT live.source_exam_entry_draft::text, live.semester, live.exam::text, live.phase, live.assessment_method,
                 live."start"::text, live."end"::text
          FROM schedule.exam_entry live
          WHERE live.source_plan_draft = ${p.toString}::uuid ORDER BY 1"""
      .as[Entry]
      .zip(sql"""SELECT live.source_exam_entry_draft::text, live_room.room::text, live_room.invigilators::text,
                   live_room.partition_by, live_room.partition_value
            FROM schedule.exam_room live_room JOIN schedule.exam_entry live ON live.id = live_room.exam_entry
            WHERE live.source_plan_draft = ${p.toString}::uuid ORDER BY 1, 2""".as[Room])

  /** Ids and physical row versions of the published rows, which change on every write. */
  private val versions: DBIO[Map[String, (String, String)]] =
    sql"""SELECT live.source_exam_entry_draft::text, live.id::text, live.ctid::text FROM schedule.exam_entry live
          WHERE live.source_plan_draft = ${plan.toString}::uuid
          UNION ALL
          SELECT live.source_exam_entry_draft::text || '/' || live_room.room, live.id::text, live_room.ctid::text
          FROM schedule.exam_room live_room JOIN schedule.exam_entry live ON live.id = live_room.exam_entry
          WHERE live.source_plan_draft = ${plan.toString}::uuid"""
      .as[(String, String, String)]
      .map(_.map((key, id, version) => key -> (id, version)).toMap)

  private val publishedAt =
    sql"SELECT published_at IS NOT NULL FROM schedule.plan_draft WHERE id = ${plan.toString}::uuid".as[Boolean].head

  private def updateEntry(e: UUID, set: String) =
    sqlu"UPDATE schedule.exam_entry_draft SET #$set WHERE id = ${e.toString}::uuid"

  private def deleteEntry(e: UUID) = sqlu"DELETE FROM schedule.exam_entry_draft WHERE id = ${e.toString}::uuid"

  private def updateRoom(e: UUID, r: UUID, set: String) =
    sqlu"""UPDATE schedule.exam_room_draft SET #$set
           WHERE exam_entry_draft = ${e.toString}::uuid AND room = ${r.toString}::uuid"""

  private def deleteRoom(e: UUID, r: UUID) =
    sqlu"""DELETE FROM schedule.exam_room_draft WHERE exam_entry_draft = ${e.toString}::uuid AND room = ${r.toString}::uuid"""

  /** The changes after the first publication. */
  private val changes = DBIO.seq(
    updateEntry(moved, """"start" = "start" + interval '1 hour', "end" = "end" + interval '1 hour'"""),
    updateRoom(moved, hall, "invigilators = '{abe}', partition_by = NULL, partition_value = NULL"),
    deleteRoom(moved, lab),
    roomDraft(moved, annex, List("abr")),
    deleteEntry(removed),
    entryDraft(added, plan, math, 16, 17, Some("written-exam"), "wise_2"),
    roomDraft(added, hall)
  )

  test("the first publication copies all drafts with their rooms and leaves no unpublished changes") {
    val (before, count, after, state, plannedAt, other) = rollback(for {
      _      <- fixtures
      before <- hasUnpublishedChanges(plan)
      count  <- publishExamPlan(plan)
      after  <- hasUnpublishedChanges(plan)
      state  <- drafts(plan).zip(published(plan))
      at     <- publishedAt
      other  <- published(otherPlan)
    } yield (before, count, after, state, at, other))

    before shouldBe true
    count shouldBe 1
    after shouldBe false
    state._2 shouldBe state._1
    state._2._1.map(_._1) shouldBe Vector(moved, removed, kept).map(_.toString)
    plannedAt shouldBe true
    other shouldBe (Vector.empty, Vector.empty)
  }

  test("republishing an unchanged plan writes no published row") {
    val (first, second) = rollback(for {
      _      <- fixtures
      _      <- publishExamPlan(plan)
      first  <- versions
      _      <- publishExamPlan(plan)
      second <- versions
    } yield (first, second))

    first.size shouldBe 7
    second shouldBe first
  }

  test("changes, additions and removals are published, keeping ids and rewriting only what changed") {
    val (first, second, state, pending) = rollback(for {
      _       <- fixtures
      _       <- publishExamPlan(plan)
      first   <- versions
      _       <- changes
      _       <- publishExamPlan(plan)
      second  <- versions
      state   <- drafts(plan).zip(published(plan))
      pending <- hasUnpublishedChanges(plan)
    } yield (first, second, state, pending))

    state._2 shouldBe state._1
    pending shouldBe false
    second.keySet shouldBe Set(
      s"$moved",
      s"$moved/$hall",
      s"$moved/$annex",
      s"$kept",
      s"$kept/$lab",
      s"$added",
      s"$added/$hall"
    )
    // the copy of the removed draft is deleted with its rooms
    first.keySet -- second.keySet shouldBe Set(s"$moved/$lab", s"$removed", s"$removed/$hall")
    second(s"$moved")._1 shouldBe first(s"$moved")._1
    second(s"$moved")._2 should not be first(s"$moved")._2
    second(s"$moved/$hall")._2 should not be first(s"$moved/$hall")._2
    second(s"$kept") shouldBe first(s"$kept")
    second(s"$kept/$lab") shouldBe first(s"$kept/$lab")
  }

  test("a plan without drafts removes its published copies, but not those of other plans") {
    val (state, other, pending) = rollback(for {
      _       <- fixtures
      _       <- publishExamPlan(plan)
      _       <- publishExamPlan(otherPlan)
      _       <- sqlu"DELETE FROM schedule.exam_entry_draft WHERE plan_draft = ${plan.toString}::uuid"
      _       <- publishExamPlan(plan)
      state   <- published(plan)
      other   <- published(otherPlan)
      pending <- hasUnpublishedChanges(plan)
    } yield (state, other, pending))

    state shouldBe (Vector.empty, Vector.empty)
    other._1.map(_._1) shouldBe Vector(foreign.toString)
    other._2.size shouldBe 1
    pending shouldBe false
  }

  private val unpublishedChanges = List(
    "a moved appointment"           -> updateEntry(kept, """"end" = "end" + interval '1 minute'"""),
    "another phase"                 -> updateEntry(kept, "phase = 'wise_1'"),
    "a cleared assessment method"   -> updateEntry(kept, "assessment_method = NULL"),
    "a set assessment method"       -> updateEntry(removed, "assessment_method = 'written-exam'"),
    "another exam"                  -> updateEntry(kept, s"exam = '$math'"),
    "an added appointment"          -> entryDraft(added, plan, math, 16, 17),
    "a deleted appointment"         -> deleteEntry(removed),
    "an added room"                 -> roomDraft(kept, hall),
    "a removed room"                -> deleteRoom(moved, lab),
    "a replaced room"               -> updateRoom(kept, lab, s"room = '$annex'"),
    "changed invigilators"          -> updateRoom(kept, lab, "invigilators = '{aaz,abr}'"),
    "removed invigilators"          -> updateRoom(kept, lab, "invigilators = '{}'"),
    "a cleared partition"           -> updateRoom(moved, hall, "partition_by = NULL, partition_value = NULL"),
    "another participant partition" -> updateRoom(moved, hall, "partition_value = 'L-Z'")
  )

  private val draftOnlyChanges = List(
    "changed expected participants"    -> updateEntry(kept, "expected_participants = 42"),
    "changed seats and required count" -> updateRoom(kept, lab, "allocated_seats = 99, required_invigilators = 5"),
    "a changed draft of another plan"  -> updateEntry(foreign, """"end" = "end" + interval '1 hour'""")
  )

  for (label, change) <- unpublishedChanges do
    test(s"$label is an unpublished change") {
      rollback(fixtures >> publishExamPlan(plan) >> change >> hasUnpublishedChanges(plan)) shouldBe true
    }

  for (label, change) <- draftOnlyChanges do
    test(s"$label is no unpublished change") {
      rollback(fixtures >> publishExamPlan(plan) >> change >> hasUnpublishedChanges(plan)) shouldBe false
    }

  test("a never published plan without drafts has no unpublished changes") {
    rollback(examPlan(plan, "wise_2098") >> hasUnpublishedChanges(plan)) shouldBe false
  }
}
