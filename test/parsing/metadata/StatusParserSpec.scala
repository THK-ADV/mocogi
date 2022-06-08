package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.metadata.StatusParser.{statusFileParser, statusParser}
import parsing.types.Status
import parsing.{ParserSpecHelper, withResFile}

class StatusParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Status Parser" should {
    "parse status file" when {
      "parse a single status" in {
        val input =
          """active:
            |  de_label: Aktiv""".stripMargin
        val (res, rest) = statusFileParser.run(input)
        assert(
          res.value == List(Status("active", "Aktiv"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple status" in {
        val input =
          """active:
            |  de_label: Aktiv
            |
            |inactive:
            |  de_label: Inaktiv""".stripMargin
        val (res, rest) = statusFileParser.run(input)
        assert(
          res.value == List(
            Status("active", "Aktiv"),
            Status("inactive", "Inaktiv")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all status in status.yaml" in {
        val (res, rest) = withResFile("status.yaml")(statusFileParser.run)
        assert(
          res.value == List(
            Status("active", "Aktiv"),
            Status("inactive", "Inaktiv")
          )
        )
        assert(rest.isEmpty)
      }
    }

    "parse status" should {
      "return a valid status" in {
        val (res1, rest1) = statusParser.run("status: status.active\n")
        assert(res1.value == Status("active", "Aktiv"))
        assert(rest1.isEmpty)

        val (res2, rest2) = statusParser.run("status: status.inactive\n")
        assert(res2.value == Status("inactive", "Inaktiv"))
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
