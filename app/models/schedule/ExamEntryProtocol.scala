package models.schedule

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

import models.core.ExamPhases.ExamPhase
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

/** Creates an exam appointment of any exam in an exam phase. Module and examiner data are resolved from the exam. */
case class ExamEntryCreate(
    exam: UUID,
    phase: String,
    assessmentMethod: String,
    start: Instant,
    end: Instant,
    expectedParticipants: Option[Int]
)

/** Replaces the planning fields. All keys are required; legacy appointments may have no selected assessment method. */
case class ExamEntryUpdate(
    start: Instant,
    end: Instant,
    expectedParticipants: Option[Int],
    assessmentMethod: Option[String]
)

/**
 * Room of an exam appointment with its seats and invigilators. Unknown counts are `None`. The invigilators must not
 * exceed a known `requiredInvigilators`. `partitionBy` and `partitionValue` are free labels of the participants in this
 * room, e.g. `Nachname` and `A-Me`, both `None` if unspecified.
 */
case class ExamRoomAllocation(
    room: UUID,
    allocatedSeats: Option[Int],
    requiredInvigilators: Option[Int],
    invigilators: List[String],
    partitionBy: Option[String],
    partitionValue: Option[String]
)

object ExamEntryCreate {
  given Reads[ExamEntryCreate] = Json.reads

  private val phases = ExamPhase.all.toList.map(_.id).toSet - ExamPhase.none.id

  /** Throws an `IllegalArgumentException` for an invalid appointment or an unknown phase. */
  def validate(c: ExamEntryCreate): Unit = {
    require(phases(c.phase), s"unknown exam phase ${c.phase}")
    ExamEntryUpdate.validate(c.start, c.end, c.expectedParticipants)
  }
}

object ExamEntryUpdate {
  given Reads[ExamEntryUpdate] =
    (__ \ "start")
      .read[Instant]
      .and((__ \ "end").read[Instant])
      .and((__ \ "expectedParticipants").read(Reads.optionWithNull[Int]))
      .and((__ \ "assessmentMethod").read(Reads.optionWithNull[String]))(ExamEntryUpdate.apply)

  private val zone = ZoneId.of("Europe/Berlin")

  /**
   * Throws an `IllegalArgumentException` unless the appointment ends after it starts on the same day in Berlin and the
   * expected participants are not negative.
   */
  def validate(start: Instant, end: Instant, expectedParticipants: Option[Int]): Unit = {
    require(end.isAfter(start), "end must be after start")
    require(start.atZone(zone).toLocalDate == end.atZone(zone).toLocalDate, "start and end must be on the same day")
    require(expectedParticipants.forall(_ >= 0), "expectedParticipants must not be negative")
  }
}

object ExamRoomAllocation {
  given Format[ExamRoomAllocation] = Json.format
}

/**
 * Selection of the planning pool of a `semester`: offers in one of `phases` with one of `assessmentMethods`, where an
 * empty list matches any. `availableInvigilators` are the person ids automatic invigilator assignment chooses from and
 * do not affect the pool. Lists are read as sorted sets and default to empty.
 */
case class ExamPoolConfig(
    semester: String,
    phases: List[String] = Nil,
    assessmentMethods: List[String] = Nil,
    availableInvigilators: List[String] = Nil
)

object ExamPoolConfig {
  given Reads[ExamPoolConfig] =
    Json
      .using[Json.WithDefaultValues]
      .reads[ExamPoolConfig]
      .map(c =>
        c.copy(
          phases = c.phases.distinct.sorted,
          assessmentMethods = c.assessmentMethods.distinct.sorted,
          availableInvigilators = c.availableInvigilators.distinct.sorted
        )
      )

  given OWrites[ExamPoolConfig] = Json.writes

  /** Configuration of a plan without a stored one: written exams of all phases. */
  def default(semester: String) =
    ExamPoolConfig(semester, Nil, List("e-exam", "written-exam", "written-exam-answer-choice-method"))

  private val phases = ExamPhase.all.toList.map(_.id).toSet - ExamPhase.none.id

  /** Throws an `IllegalArgumentException` for an invalid semester or unknown phases. */
  def validate(c: ExamPoolConfig): Unit = {
    require(c.semester.matches("(sose|wise)_\\d{4}"), s"invalid semester ${c.semester}")
    val unknown = c.phases.filterNot(phases)
    require(unknown.isEmpty, s"unknown exam phases: ${unknown.mkString(", ")}")
  }
}
