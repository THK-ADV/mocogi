package parsing.metadata.file

import basedata.Location
import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.{ParserSpecHelper, withFile0}

class LocationFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[LocationFileParser]).fileParser

  "A Location File Parser" should {
    "parse location file" when {
      "parse a single location" in {
        val input =
          """gm:
            |  de_label: Gummersbach
            |  en_label: Gummersbach""".stripMargin
        val (res, rest) = parser.parse(input)
        assert(
          res.value == List(Location("gm", "Gummersbach", "Gummersbach"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple locations" in {
        val input =
          """gm:
            |  de_label: Gummersbach
            |  en_label: Gummersbach
            |
            |dz:
            |  de_label: Deutz
            |  en_label: Deutz
            |
            |remote:
            |  de_label: Remote / Online
            |  en_label: remote / online
            |
            |other:
            |  de_label: Sonstige / Variabel
            |  en_label: other / variable""".stripMargin
        val (res, rest) = parser.parse(input)
        assert(
          res.value == List(
            Location("gm", "Gummersbach", "Gummersbach"),
            Location("dz", "Deutz", "Deutz"),
            Location("remote", "Remote / Online", "remote / online"),
            Location("other", "Sonstige / Variabel", "other / variable")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all locations in location.yaml" in {
        val (res, rest) =
          withFile0("test/parsing/res/location.yaml")(parser.parse)
        assert(
          res.value == List(
            Location("gm", "Gummersbach", "--"),
            Location("dz", "Deutz", "--"),
            Location("remote", "Remote / Online", "--"),
            Location("other", "Sonstige / Variabel", "--")
          )
        )
        assert(rest.isEmpty)
      }
    }
  }
}
