package parsing.metadata

import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.types.Season
import parsing.{ParserSpecHelper, withFile0}

class SeasonParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[SeasonParser])

  val seasonFileParser = parser.fileParser
  val seasonParser = parser.parser

  "A Season Parser" should {
    "parse season file" when {
      "parse a single season" in {
        val input =
          """ss:
            |  de_label: Sommersemester""".stripMargin
        val (res, rest) = seasonFileParser.parse(input)
        assert(
          res.value == List(Season("ss", "Sommersemester"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple seasons" in {
        val input =
          """ws:
            |  de_label: Wintersemester
            |
            |ss:
            |  de_label: Sommersemester
            |
            |ws_ss:
            |  de_label: Winter- und Sommersemester""".stripMargin
        val (res, rest) = seasonFileParser.parse(input)
        assert(
          res.value == List(
            Season("ws", "Wintersemester"),
            Season("ss", "Sommersemester"),
            Season("ws_ss", "Winter- und Sommersemester")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all seasons in season.yaml" in {
        val (res, rest) =
          withFile0("test/parsing/res/season.yaml")(seasonFileParser.parse)
        assert(
          res.value == List(
            Season("ws", "Wintersemester"),
            Season("ss", "Sommersemester"),
            Season("ws_ss", "Winter- und Sommersemester")
          )
        )
        assert(rest.isEmpty)
      }
    }
    "parse season" in {
      val (res1, rest1) = seasonParser.parse("frequency: season.ws\n")
      assert(res1.value == Season("ws", "Wintersemester"))
      assert(rest1.isEmpty)

      val (res2, rest2) = seasonParser.parse("frequency: season.ss\n")
      assert(res2.value == Season("ss", "Sommersemester"))
      assert(rest2.isEmpty)

      val (res3, rest3) = seasonParser.parse("frequency: season.ws_ss\n")
      assert(res3.value == Season("ws_ss", "Winter- und Sommersemester"))
      assert(rest3.isEmpty)
    }
  }
}
