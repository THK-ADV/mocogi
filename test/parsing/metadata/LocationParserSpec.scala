package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{ParserSpecHelper, withResFile}
import parsing.metadata.LocationParser.{locationFileParser, locationParser}
import parsing.types.Location

class LocationParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Location Parser" should {
    "parse location file" when {
      "parse a single location" in {
        val input =
          """gm:
            |  de_label: Gummersbach""".stripMargin
        val (res, rest) = locationFileParser.parse(input)
        assert(
          res.value == List(Location("gm", "Gummersbach"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple locations" in {
        val input =
          """gm:
            |  de_label: Gummersbach
            |
            |dz:
            |  de_label: Deutz
            |
            |remote:
            |  de_label: Remote / Online
            |
            |other:
            |  de_label: Sonstige / Variabel""".stripMargin
        val (res, rest) = locationFileParser.parse(input)
        assert(
          res.value == List(
            Location("gm", "Gummersbach"),
            Location("dz", "Deutz"),
            Location("remote", "Remote / Online"),
            Location("other", "Sonstige / Variabel")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all locations in location.yaml" in {
        val (res, rest) = withResFile("location.yaml")(locationFileParser.parse)
        assert(
          res.value == List(
            Location("gm", "Gummersbach"),
            Location("dz", "Deutz"),
            Location("remote", "Remote / Online"),
            Location("other", "Sonstige / Variabel")
          )
        )
        assert(rest.isEmpty)
      }
    }

    "parse location" should {
      "return a valid location" in {
        val (res1, rest1) = locationParser.parse("location: location.gm\n")
        assert(res1.value == Location("gm", "Gummersbach"))
        assert(rest1.isEmpty)

        val (res2, rest2) = locationParser.parse("location: location.dz\n")
        assert(res2.value == Location("dz", "Deutz"))
        assert(rest2.isEmpty)
      }

      "fail if the location is unknown" in {
        assertError(
          locationParser,
          "location: location.iwz\n",
          "location.gm or location.dz or location.remote or location.other"
        )
      }
    }
  }
}
