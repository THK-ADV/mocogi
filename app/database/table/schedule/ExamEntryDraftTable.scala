package database.table.schedule

import java.time.Instant
import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*

/** Exam appointment of an exam plan draft. Reads use `functions.sql`. */
private[database] final class ExamEntryDraftTable(tag: Tag)
    extends Table[(UUID, UUID, UUID, String, Instant, Instant, Option[Int], Option[String])](
      tag,
      Some(Schema.Schedule.name),
      "exam_entry_draft"
    ) {

  def id = column[UUID]("id", O.PrimaryKey)

  def planDraft = column[UUID]("plan_draft")

  def exam = column[UUID]("exam")

  def phase = column[String]("phase")

  def start = column[Instant]("start")

  def end = column[Instant]("end")

  def expectedParticipants = column[Option[Int]]("expected_participants")

  def assessmentMethod = column[Option[String]]("assessment_method")

  override def * = (id, planDraft, exam, phase, start, end, expectedParticipants, assessmentMethod)
}
