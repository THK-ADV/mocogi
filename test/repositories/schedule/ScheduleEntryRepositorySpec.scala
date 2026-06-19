package database.repo.schedule

import java.time.Instant

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ScheduleEntryRepositorySpec extends AnyWordSpec with Matchers {

  "ScheduleEntryRepository.setSeriesTimes" should {
    "apply the new times to a series entry while preserving its dates" in {
      val seriesStart = Instant.parse("2026-04-27T12:00:00Z")
      val seriesEnd   = Instant.parse("2026-04-27T14:00:00Z")
      val newStart    = Instant.parse("2026-04-20T09:00:00Z")
      val newEnd      = Instant.parse("2026-04-20T11:00:00Z")

      val setSeriesTimes       = ScheduleEntryRepository.setSeriesTimes(newStart, newEnd)
      val (nextStart, nextEnd) = setSeriesTimes(seriesStart, seriesEnd)

      nextStart shouldBe Instant.parse("2026-04-27T09:00:00Z")
      nextEnd shouldBe Instant.parse("2026-04-27T11:00:00Z")
    }

    "use Europe/Berlin local time instead of a fixed instant delta" in {
      val seriesStart = Instant.parse("2026-03-30T12:00:00Z")
      val seriesEnd   = Instant.parse("2026-03-30T14:00:00Z")
      val newStart    = Instant.parse("2026-03-23T09:00:00Z")
      val newEnd      = Instant.parse("2026-03-23T11:00:00Z")

      val setSeriesTimes       = ScheduleEntryRepository.setSeriesTimes(newStart, newEnd)
      val (nextStart, nextEnd) = setSeriesTimes(seriesStart, seriesEnd)

      nextStart shouldBe Instant.parse("2026-03-30T08:00:00Z")
      nextEnd shouldBe Instant.parse("2026-03-30T10:00:00Z")
    }

    "apply the new end time to the series end date" in {
      val seriesStart = Instant.parse("2026-04-27T08:00:00Z")
      val seriesEnd   = Instant.parse("2026-04-28T06:00:00Z")
      val newStart    = Instant.parse("2026-04-20T20:00:00Z")
      val newEnd      = Instant.parse("2026-04-20T23:00:00Z")

      val setSeriesTimes       = ScheduleEntryRepository.setSeriesTimes(newStart, newEnd)
      val (nextStart, nextEnd) = setSeriesTimes(seriesStart, seriesEnd)

      nextStart shouldBe Instant.parse("2026-04-27T20:00:00Z")
      nextEnd shouldBe Instant.parse("2026-04-27T23:00:00Z")
    }
  }
}
