package parsing.metadata

import helper.{FakeApplication, FakeLocations}
import models.core.ModuleLocation
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper

class ModuleLocationParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeLocations {

  val parser = app.injector
    .instanceOf(classOf[ModuleLocationParser])
    .parser

  "A Location Parser" should {
    "parse a valid location" in {
      val (res1, rest1) = parser.parse("location: location.gm\n")
      assert(res1.value == ModuleLocation("gm", "Gummersbach", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) = parser.parse("location: location.dz\n")
      assert(res2.value == ModuleLocation("dz", "Deutz", "--"))
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
