package models.schedule

import java.time.Instant
import java.util.UUID

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class InvigilationDutySpec extends AnyFunSuite with Matchers {

  private def uuid(n: Int) = UUID.fromString(f"00000000-0000-0000-0000-$n%012d")

  private val roomA = uuid(101)
  private val roomB = uuid(102)

  /** Duty of appointment `entry` in `room` from `from` to `to` o'clock on the same day. */
  private def duty(entry: Int, from: Int, to: Int, required: Option[Int], invigilators: String*)(room: UUID = roomA) =
    InvigilationDuty(
      uuid(entry),
      room,
      Instant.parse(f"2026-02-02T$from%02d:00:00Z"),
      Instant.parse(f"2026-02-02T$to%02d:00:00Z"),
      required,
      invigilators.toList
    )

  private def all(duties: InvigilationDuty*) = duties.map(d => InvigilationDutyRef(d.entry, d.room)).toSet

  private def invigilators(changed: List[InvigilationDuty]) = changed.map(d => (d.entry, d.room) -> d.invigilators)

  test("fills the open demand with the candidates of the fewest duties, ties broken by id, keeping existing ones") {
    val busy = duty(1, 8, 9, Some(2), "a", "b")()
    val open = duty(2, 10, 11, Some(3), "z")()

    val changed = InvigilationDuty.assign(Seq(busy, open), Seq("a", "b", "c", "d"), all(open))

    changed shouldBe List(open.copy(invigilators = List("c", "d", "z")))
  }

  test("distributes sequential duties evenly as each assignment counts towards the load") {
    val duties = (1 to 4).map(n => duty(n, 7 + n, 8 + n, Some(1))())

    val changed = InvigilationDuty.assign(duties, Seq("b", "a"), all(duties*))

    changed.map(_.invigilators) shouldBe List(List("a"), List("b"), List("a"), List("b"))
  }

  test("never assigns a candidate to overlapping duties, also not in another room, but to adjacent ones") {
    val first    = duty(1, 8, 10, Some(1))(roomA)
    val parallel = duty(2, 9, 11, Some(1))(roomB)
    val adjacent = duty(3, 10, 12, Some(1))(roomB)

    val changed = InvigilationDuty.assign(Seq(first, parallel, adjacent), Seq("a", "b"), all(first, parallel, adjacent))

    invigilators(changed) shouldBe List(
      (uuid(1), roomA) -> List("a"),
      (uuid(2), roomB) -> List("b"),
      // a and b have one duty each, a's ends when this one starts, b's overlaps it
      (uuid(3), roomB) -> List("a")
    )
  }

  test("does not add a candidate to a duty they already have") {
    val d = duty(1, 8, 10, Some(2), "a")()

    InvigilationDuty.assign(Seq(d), Seq("a"), all(d)) shouldBe Nil
  }

  test("fills only partially if too few candidates are free and leaves full duties out of the result") {
    val parallel = duty(1, 8, 10, Some(1), "a")(roomA)
    val open     = duty(2, 9, 11, Some(3))(roomB)
    val full     = duty(3, 12, 13, Some(1), "b")()

    val changed = InvigilationDuty.assign(Seq(parallel, open, full), Seq("a", "b", "c"), all(parallel, open, full))

    invigilators(changed) shouldBe List((uuid(2), roomB) -> List("b", "c"))
  }

  test("unselected duties and those of unknown demand stay unchanged but count towards load and overlaps") {
    val unselected = duty(1, 8, 10, Some(2), "a")(roomA)
    val unknown    = duty(2, 8, 10, None, "b")(roomB)
    val open       = duty(3, 9, 11, Some(2))()
    val later      = duty(4, 12, 13, Some(1))()

    val changed =
      InvigilationDuty.assign(Seq(unselected, unknown, open, later), Seq("a", "b", "c"), all(unknown, open, later))

    // a and b overlap with open, c then has a duty and a, b the fewest for later
    invigilators(changed) shouldBe List((uuid(3), roomA) -> List("c"), (uuid(4), roomA) -> List("a"))
  }

  test("invigilators that are no candidates count towards the demand, but are neither chosen nor loaded") {
    val external = duty(1, 8, 10, Some(2), "x")()
    val other    = duty(2, 11, 12, Some(1), "x")()

    val changed = InvigilationDuty.assign(Seq(external, other), Seq("a"), all(external, other))

    changed shouldBe List(external.copy(invigilators = List("a", "x")))
  }

  test("an empty candidate list changes nothing") {
    val d = duty(1, 8, 10, Some(2))()

    InvigilationDuty.assign(Seq(d), Nil, all(d)) shouldBe Nil
  }
}
