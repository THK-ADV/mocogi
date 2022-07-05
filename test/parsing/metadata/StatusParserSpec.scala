package parsing.metadata

import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.types.Status
import parsing.{ParserSpecHelper, withFile0}

class StatusParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[StatusParser])

  val statusFileParser = parser.fileParser
  val statusParser = parser.parser

  "A Status Parser" should {
    "parse status file" when {
      "parse a single status" in {
        val input =
          """active:
            |  de_label: Aktiv
            |  en_label: active""".stripMargin
        val (res, rest) = statusFileParser.parse(input)
        assert(
          res.value == List(Status("active", "Aktiv", "active"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple status" in {
        val input =
          """active:
            |  de_label: Aktiv
            |  en_label: active
            |
            |inactive:
            |  de_label: Inaktiv
            |  en_label: inactive""".stripMargin
        val (res, rest) = statusFileParser.parse(input)
        assert(
          res.value == List(
            Status("active", "Aktiv", "active"),
            Status("inactive", "Inaktiv", "inactive")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all status in status.yaml" in {
        val (res, rest) = withFile0("test/parsing/res/status.yaml")(statusFileParser.parse)
        assert(
          res.value == List(
            Status("active", "Aktiv", "--"),
            Status("inactive", "Inaktiv", "--")
          )
        )
        assert(rest.isEmpty)
      }
    }

    "parse status" should {
      "return a valid status" in {
        val (res1, rest1) = statusParser.parse("status: status.active\n")
        assert(res1.value == Status("active", "Aktiv", "--"))
        assert(rest1.isEmpty)

        val (res2, rest2) = statusParser.parse("status: status.inactive\n")
        assert(res2.value == Status("inactive", "Inaktiv", "--"))
        assert(rest2.isEmpty)
      }

      "fail if the status is unknown" in {
        assertError(
          statusParser,
          "status: status.unknown\n",
          "status.active or status.inactive"
        )
      }
    }
  }
}
