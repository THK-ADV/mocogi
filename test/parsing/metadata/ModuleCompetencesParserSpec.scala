package parsing.metadata

import helper.FakeCompetences
import models.core.ModuleCompetence
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleCompetencesParser.competencesParser

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
      val (res, rest) = competencesParser.parse(input)
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

    "parse no competences if empty" in {
      val input = "competences:"
      val (res, rest) = competencesParser.parse(input)
      assert(rest.isEmpty)
      assert(res.value.isEmpty)
    }
  }
}
