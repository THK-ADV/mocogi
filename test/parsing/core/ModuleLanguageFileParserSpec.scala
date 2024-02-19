package parsing.core

import helper.FakeApplication
import models.core.ModuleLanguage
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.{ParserSpecHelper, withFile0}

final class ModuleLanguageFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = LanguageFileParser.fileParser

  "A Language File Parser" should {
    "parse a single language" in {
      val input =
        """de:
          |  de_label: Deutsch
          |  en_label: german""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(ModuleLanguage("de", "Deutsch", "german"))
      )
      assert(rest.isEmpty)
    }

    "parse multiple languages" in {
      val input =
        """de:
          |  de_label: Deutsch
          |  en_label: german
          |
          |en:
          |  de_label: Englisch
          |  en_label: english
          |
          |de_en:
          |  de_label: Deutsch und Englisch
          |  en_label: german and english""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          ModuleLanguage("de", "Deutsch", "german"),
          ModuleLanguage("en", "Englisch", "english"),
          ModuleLanguage("de_en", "Deutsch und Englisch", "german and english")
        )
      )
      assert(rest.isEmpty)
    }

    "parse all languages in lang.yaml" in {
      val (res, rest) = withFile0("test/parsing/res/lang.yaml")(parser.parse)
      assert(
        res.value == List(
          ModuleLanguage("de", "Deutsch", "--"),
          ModuleLanguage("en", "Englisch", "--"),
          ModuleLanguage("de_en", "Deutsch und Englisch", "--")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
