package parsing.metadata

import helper.FakeLanguages
import models.core.ModuleLanguage
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.ParserSpecHelper

class ModuleLanguageParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues with FakeLanguages {

  val parser = ModuleLanguageParser.parser
  val raw    = ModuleLanguageParser.raw

  "A Language Parser" should {
    "parse a valid language" in {
      val (res1, rest1) = parser.parse("language: lang.de\n")
      assert(res1.value == ModuleLanguage("de", "Deutsch", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) = parser.parse("language: lang.en\n")
      assert(res2.value == ModuleLanguage("en", "Englisch", "--"))
      assert(rest2.isEmpty)

      val (res3, rest3) = parser.parse("language: lang.de_en\n")
      assert(
        res3.value == ModuleLanguage("de_en", "Deutsch und Englisch", "--")
      )
      assert(rest3.isEmpty)
    }

    "parse a valid language raw" in {
      val (res1, rest1) = raw.parse("language: lang.de\n")
      assert(res1.value == "de")
      assert(rest1.isEmpty)

      val (res2, rest2) = raw.parse("language: lang.en\n")
      assert(res2.value == "en")
      assert(rest2.isEmpty)

      val (res3, rest3) = raw.parse("language: lang.de_en\n")
      assert(res3.value == "de_en")
      assert(rest3.isEmpty)
    }

    "fail if the language is unknown" in {
      assertError(
        parser,
        "language: lang.ger\n",
        "lang.en or lang.de_en or lang.de",
        Some("lang.ger\n")
      )
    }
  }
}
