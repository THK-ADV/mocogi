package database.repo.schedule

import java.time.Instant
import java.util.UUID

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class SchedulePlanDraftRepositorySpec extends AnyWordSpec with Matchers {

  private val entryId = UUID.fromString("00000000-0000-0000-0000-000000000001")

  "SchedulePlanDraftRepository.validateSemesterEntries" should {
    "accept entries fully contained in the semester" in {
      val entries = Seq(
        (entryId, Instant.parse("2026-02-28T23:00:00Z"), Instant.parse("2026-08-31T22:00:00Z"))
      )

      SchedulePlanDraftRepository.validateSemesterEntries("sose_2026", entries) shouldBe Right(())
    }

    "reject an entry starting before the semester" in {
      val entries = Seq(
        (entryId, Instant.parse("2026-02-28T22:59:59Z"), Instant.parse("2026-03-01T00:00:00Z"))
      )

      SchedulePlanDraftRepository.validateSemesterEntries("sose_2026", entries) shouldBe
        Left(s"schedule entry draft $entryId is outside semester sose_2026")
    }

    "reject an entry starting at the exclusive semester end" in {
      val entries = Seq(
        (entryId, Instant.parse("2026-08-31T22:00:00Z"), Instant.parse("2026-08-31T23:00:00Z"))
      )

      SchedulePlanDraftRepository.validateSemesterEntries("sose_2026", entries) shouldBe
        Left(s"schedule entry draft $entryId is outside semester sose_2026")
    }

    "reject an entry ending after the semester" in {
      val entries = Seq(
        (entryId, Instant.parse("2026-08-31T21:00:00Z"), Instant.parse("2026-08-31T22:00:01Z"))
      )

      SchedulePlanDraftRepository.validateSemesterEntries("sose_2026", entries) shouldBe
        Left(s"schedule entry draft $entryId is outside semester sose_2026")
    }

    "reject an entry without a positive duration" in {
      val timestamp = Instant.parse("2026-04-01T10:00:00Z")
      val entries   = Seq((entryId, timestamp, timestamp))

      SchedulePlanDraftRepository.validateSemesterEntries("sose_2026", entries) shouldBe
        Left(s"schedule entry draft $entryId is outside semester sose_2026")
    }
  }
}
