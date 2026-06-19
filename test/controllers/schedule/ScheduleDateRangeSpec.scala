package controllers.schedule

import java.sql.Timestamp
import java.time.Instant

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.FakeRequest
import play.api.test.Helpers.BAD_REQUEST

final class ScheduleDateRangeSpec extends AnyWordSpec with Matchers {

  "ScheduleDateRange.resolve" should {
    "resolve from and to epoch millisecond parameters" in {
      val from    = Instant.parse("2026-04-01T00:00:00Z")
      val to      = Instant.parse("2026-10-01T00:00:00Z")
      val request = FakeRequest("GET", s"/?from=${from.toEpochMilli}&to=${to.toEpochMilli}")

      ScheduleDateRange.resolve(request) shouldBe Right((Timestamp.from(from), Timestamp.from(to)))
    }

    "require from and to together" in {
      val request = FakeRequest("GET", "/?from=1775001600000")

      ScheduleDateRange.resolve(request).left.map(_.header.status) shouldBe Left(BAD_REQUEST)
    }
  }
}
