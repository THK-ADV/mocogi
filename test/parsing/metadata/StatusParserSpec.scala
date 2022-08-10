package parsing.metadata

import helper.{FakeApplication, FakeStatus}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper
import parsing.types.Status

class StatusParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeStatus {

  val parser = app.injector.instanceOf(classOf[StatusParser]).parser

  "A Status Parser" should {
    "parse a valid status" in {
      val (res1, rest1) = parser.parse("status: status.active\n")
      assert(res1.value == Status("active", "Aktiv", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) = parser.parse("status: status.inactive\n")
      assert(res2.value == Status("inactive", "Inaktiv", "--"))
      assert(rest2.isEmpty)
    }

    "fail if the status is unknown" in {
      assertError(
        parser,
        "status: status.unknown\n",
        "status.active or status.inactive",
        Some("status.unknown\n")
      )
    }
  }
}
