package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.metadata.LanguageParser.{langFileParser, languageParser}
import parsing.types.Language
import parsing.{ParserSpecHelper, withResFile}

class LanguageParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Language Parser" should {
    "parse language file" when {
      "parse a single language" in {
        val input =
          """de:
            |  de_label: Deutsch""".stripMargin
        val (res, rest) = langFileParser.run(input)
        assert(
          res.value == List(Language("de", "Deutsch"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple languages" in {
        val input =
          """de:
            |  de_label: Deutsch
            |
            |en:
            |  de_label: Englisch
            |
            |de_en:
            |  de_label: Deutsch und Englisch""".stripMargin
        val (res, rest) = langFileParser.run(input)
        assert(
          res.value == List(
            Language("de", "Deutsch"),
            Language("en", "Englisch"),
            Language("de_en", "Deutsch und Englisch")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all languages in lang.yaml" in {
        val (res, rest) = withResFile("lang.yaml")(langFileParser.run)
        assert(
          res.value == List(
            Language("de", "Deutsch"),
            Language("en", "Englisch"),
            Language("de_en", "Deutsch und Englisch")
          )
        )
        assert(rest.isEmpty)
      }
    }
  }

  "parse language" should {
    "return a valid language" in {
      val (res1, rest1) = languageParser.run("language: lang.de\n")
      assert(res1.value == Language("de", "Deutsch"))
      assert(rest1.isEmpty)

      val (res2, rest2) = languageParser.run("language: lang.en\n")
      assert(res2.value == Language("en", "Englisch"))
      assert(rest2.isEmpty)

      val (res3, rest3) = languageParser.run("language: lang.de_en\n")
      assert(res3.value == Language("de_en", "Deutsch und Englisch"))
      assert(rest3.isEmpty)
    }

    "fail if the language is unknown" in {
      assertError(
        languageParser,
        "language: lang.ger\n",
        "lang.de or lang.en or lang.de_en"
      )
    }
  }
}
