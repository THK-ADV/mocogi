package parsing.metadata

import helper.FakeGlobalCriteria
import models.core.ModuleGlobalCriteria
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleGlobalCriteriaParser.globalCriteriaParser

final class ModuleGlobalCriteriaParserSpec
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
          ModuleGlobalCriteria(
            "internationalization",
            "Internationalisierung",
            "...",
            "Internationalization",
            "..."
          ),
          ModuleGlobalCriteria(
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
