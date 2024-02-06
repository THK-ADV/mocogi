package parsing.metadata

import helper.{FakeApplication, FakeStatus}
import models.core.ModuleStatus
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper

class ModuleStatusParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeStatus {

  val parser = app.injector.instanceOf(classOf[ModuleStatusParser]).parser

  "A Status Parser" should {
    "parse a valid status" in {
      val (res1, rest1) = parser.parse("status: status.active\n")
      assert(res1.value == ModuleStatus("active", "Aktiv", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) = parser.parse("status: status.inactive\n")
      assert(res2.value == ModuleStatus("inactive", "Inaktiv", "--"))
      assert(rest2.isEmpty)
    }

    "fail if the status is unknown" in {
      assertError(
        parser,
        "status: status.unknown\n",
        "status.inactive or status.active",
        Some("status.unknown\n")
      )
    }
  }
}
