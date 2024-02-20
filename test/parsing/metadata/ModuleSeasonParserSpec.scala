package parsing.metadata

import helper.FakeSeasons
import models.core.Season
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper

class ModuleSeasonParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeSeasons {

  val parser = ModuleSeasonParser.parser
  val raw = ModuleSeasonParser.raw

  "A Season Parser" should {

    "parse season" in {
      val (res1, rest1) = parser.parse("frequency: season.ws\n")
      assert(res1.value == Season("ws", "Wintersemester", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) = parser.parse("frequency: season.ss\n")
      assert(res2.value == Season("ss", "Sommersemester", "--"))
      assert(rest2.isEmpty)

      val (res3, rest3) = parser.parse("frequency: season.ws_ss\n")
      assert(res3.value == Season("ws_ss", "Winter- und Sommersemester", "--"))
      assert(rest3.isEmpty)
    }

    "parse season raw" in {
      val (res1, rest1) = raw.parse("frequency: season.ws\n")
      assert(res1.value == "ws")
      assert(rest1.isEmpty)

      val (res2, rest2) = raw.parse("frequency: season.ss\n")
      assert(res2.value == "ss")
      assert(rest2.isEmpty)

      val (res3, rest3) = raw.parse("frequency: season.ws_ss\n")
      assert(res3.value == "ws_ss")
      assert(rest3.isEmpty)
    }
  }
}
