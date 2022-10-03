package parsing.metadata

import basedata.GlobalCriteria
import helper.FakeGlobalCriteria
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.GlobalCriteriaParser.globalCriteriaParser

final class GlobalCriteriaParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeGlobalCriteria {
  "A Global Criteria Parser" should {
    "parse multiple global criteria" in {
      val input =
        """global_criteria:
          |  - global_criteria.internationalization
          |  - global_criteria.digitization""".stripMargin
      val (res, rest) = globalCriteriaParser.parse(input)
      assert(rest.isEmpty)
      assert(
        res.value == List(
          GlobalCriteria(
            "internationalization",
            "Internationalisierung",
            "...",
            "Internationalization",
            "..."
          ),
          GlobalCriteria(
            "digitization",
            "Digitalisierung",
            "...",
            "Digitization",
            "..."
          )
        )
      )
    }

    "parse no global criteria if empty" in {
      val input = "global_criteria:"
      val (res, rest) = globalCriteriaParser.parse(input)
      assert(rest.isEmpty)
      assert(res.value.isEmpty)
    }
  }
}
