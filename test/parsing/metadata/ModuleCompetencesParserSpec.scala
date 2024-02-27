package parsing.metadata

import helper.FakeCompetences
import models.core.ModuleCompetence
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleCompetencesParser.{parser, raw}

final class ModuleCompetencesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeCompetences {
  "A Competences Parser" should {
    "parse multiple competences" in {
      val input =
        """competences:
          |  - competence.analyze-domains
          |  - competence.model-systems""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(rest.isEmpty)
      assert(
        res.value == List(
          ModuleCompetence(
            "analyze-domains",
            "Analyze Domains",
            "...",
            "Analyze Domains",
            "..."
          ),
          ModuleCompetence(
            "model-systems",
            "Model Systems",
            "...",
            "Model Systems",
            "..."
          )
        )
      )
    }

    "parse a competence raw" in {
      val input = "competences: competence.analyze-domains\nrest"
      val (res, rest) = raw.parse(input)
      assert(rest == "rest")
      assert(res.value == List("analyze-domains"))
    }

    "parse multiple competences raw" in {
      val input =
        """competences:
          |  - competence.analyze-domains
          |  - competence.model-systems""".stripMargin
      val (res, rest) = raw.parse(input)
      assert(rest.isEmpty)
      assert(
        res.value == List(
          "analyze-domains",
          "model-systems"
        )
      )
    }

    "parse no competences if empty" in {
      val input = "competences:"
      val (res, rest) = parser.parse(input)
      assert(rest.isEmpty)
      assert(res.value.isEmpty)
    }
  }
}
