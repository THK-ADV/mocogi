package parsing.metadata

import helper.FakeLocations
import models.core.ModuleLocation
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.ParserSpecHelper

class ModuleLocationParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues with FakeLocations {

  val parser = ModuleLocationParser.parser
  val raw    = ModuleLocationParser.raw

  "A Location Parser" should {
    "parse a valid location" in {
      val (res1, rest1) = parser.parse("location: location.gm\n")
      assert(res1.value == ModuleLocation("gm", "Gummersbach", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) = parser.parse("location: location.dz\n")
      assert(res2.value == ModuleLocation("dz", "Deutz", "--"))
      assert(rest2.isEmpty)
    }

    "parse a valid location raw" in {
      val (res1, rest1) = raw.parse("location: location.gm\n")
      assert(res1.value == "gm")
      assert(rest1.isEmpty)

      val (res2, rest2) = raw.parse("location: location.dz\n")
      assert(res2.value == "dz")
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
