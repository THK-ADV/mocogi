package parsing.metadata

import basedata.Season
import helper.{FakeApplication, FakeSeasons}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper

class SeasonParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeSeasons {

  val parser = app.injector.instanceOf(classOf[SeasonParser]).parser

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
  }
}
