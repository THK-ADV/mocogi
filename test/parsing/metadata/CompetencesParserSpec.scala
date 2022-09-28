package parsing.metadata

import helper.FakeCompetences
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.CompetencesParser.competencesParser
import parsing.types.Competence

final class CompetencesParserSpec
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
          Competence(
            "analyze-domains",
            "Analyze Domains",
            "...",
            "Analyze Domains",
            "..."
          ),
          Competence(
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
