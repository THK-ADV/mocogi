package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.metadata.SeasonParser.{seasonFileParser, seasonParser}
import parsing.types.Season
import parsing.{ParserSpecHelper, withResFile}

class SeasonParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Season Parser" should {
    "parse season file" when {
      "parse a single season" in {
        val input =
          """ss:
            |  de_label: Sommersemester""".stripMargin
        val (res, rest) = seasonFileParser.run(input)
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
        val (res, rest) = seasonFileParser.run(input)
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
        val (res, rest) = withResFile("season.yaml")(seasonFileParser.run)
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
      val (res1, rest1) = seasonParser.run("frequency: season.ws\n")
      assert(res1.value == Season("ws", "Wintersemester"))
      assert(rest1.isEmpty)

      val (res2, rest2) = seasonParser.run("frequency: season.ss\n")
      assert(res2.value == Season("ss", "Sommersemester"))
      assert(rest2.isEmpty)

      val (res3, rest3) = seasonParser.run("frequency: season.ws_ss\n")
      assert(res3.value == Season("ws_ss", "Winter- und Sommersemester"))
      assert(rest3.isEmpty)
    }
  }
}
