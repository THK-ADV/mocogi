package parsing.core

import models.core.ModuleLocation
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.withFile0
import parsing.ParserSpecHelper

class ModuleLocationFileParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues {

  val parser = LocationFileParser.parser()

  "A Location File Parser" should {
    "parse location file" when {
      "parse a single location" in {
        val input =
          """gm:
            |  de_label: Gummersbach
            |  en_label: Gummersbach""".stripMargin
        val (res, rest) = parser.parse(input)
        assert(
          res.value == List(ModuleLocation("gm", "Gummersbach", "Gummersbach"))
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
            ModuleLocation("gm", "Gummersbach", "Gummersbach"),
            ModuleLocation("dz", "Deutz", "Deutz"),
            ModuleLocation("remote", "Remote / Online", "remote / online"),
            ModuleLocation("other", "Sonstige / Variabel", "other / variable")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all locations in location.yaml" in {
        val (res, rest) =
          withFile0("test/parsing/res/location.yaml")(parser.parse)
        assert(
          res.value == List(
            ModuleLocation("gm", "Gummersbach", ""),
            ModuleLocation("dz", "Deutz", ""),
            ModuleLocation("remote", "Remote / Online", ""),
            ModuleLocation("other", "Sonstige / Variabel", "")
          )
        )
        assert(rest.isEmpty)
      }
    }
  }
}
