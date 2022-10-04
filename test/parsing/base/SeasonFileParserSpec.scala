package parsing.base

import basedata.Season
import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.{ParserSpecHelper, withFile0}

final class SeasonFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[SeasonFileParser]).fileParser

  "A Season File Parser" should {
    "parse a single season" in {
      val input =
        """ss:
          |  de_label: Sommersemester
          |  en_label: summer term""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(Season("ss", "Sommersemester", "summer term"))
      )
      assert(rest.isEmpty)
    }

    "parse multiple seasons" in {
      val input =
        """ws:
          |  de_label: Wintersemester
          |  en_label: winter term
          |
          |ss:
          |  de_label: Sommersemester
          |  en_label: summer term
          |
          |ws_ss:
          |  de_label: Winter- und Sommersemester
          |  en_label: winter- and summer term""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          Season("ws", "Wintersemester", "winter term"),
          Season("ss", "Sommersemester", "summer term"),
          Season(
            "ws_ss",
            "Winter- und Sommersemester",
            "winter- and summer term"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse all seasons in season.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/season.yaml")(parser.parse)
      assert(
        res.value == List(
          Season("ws", "Wintersemester", "--"),
          Season("ss", "Sommersemester", "--"),
          Season("ws_ss", "Winter- und Sommersemester", "--")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
