package parsing.metadata

import basedata.Location
import helper.{FakeApplication, FakeLocations}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper

class LocationParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeLocations {

  val parser = app.injector
    .instanceOf(classOf[LocationParser])
    .parser

  "A Location Parser" should {
    "parse a valid location" in {
      val (res1, rest1) = parser.parse("location: location.gm\n")
      assert(res1.value == Location("gm", "Gummersbach", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) = parser.parse("location: location.dz\n")
      assert(res2.value == Location("dz", "Deutz", "--"))
      assert(rest2.isEmpty)
    }

    "fail if the location is unknown" in {
      assertError(
        parser,
        "location: location.iwz\n",
        "location.su or location.km or location.gm or location.dz",
        Some("location.iwz\n")
      )
    }
  }
}
