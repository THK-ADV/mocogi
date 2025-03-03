package parsing.metadata

import models.core.ModuleGlobalCriteria
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.metadata.ModuleGlobalCriteriaParser.parser
import parsing.metadata.ModuleGlobalCriteriaParser.raw
import parsing.ParserSpecHelper

final class ModuleGlobalCriteriaParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues {
  "A Global Criteria Parser" should {
    "parse multiple global criteria" in {
      val input =
        """global_criteria:
          |  - global_criteria.internationalization
          |  - global_criteria.digitization""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(rest.isEmpty)
      assert(
        res.value == List(
          ModuleGlobalCriteria(
            "internationalization",
            "",
            "",
            "",
            ""
          ),
          ModuleGlobalCriteria(
            "digitization",
            "",
            "",
            "",
            ""
          )
        )
      )
    }

    "parse multiple global criteria raw" in {
      val input =
        """global_criteria:
          |  - global_criteria.internationalization
          |  - global_criteria.digitization""".stripMargin
      val (res, rest) = raw.parse(input)
      assert(rest.isEmpty)
      assert(
        res.value == List(
          "internationalization",
          "digitization"
        )
      )
    }

    "parse global criteria raw" in {
      val input       = "global_criteria: global_criteria.internationalization"
      val (res, rest) = raw.parse(input)
      assert(rest.isEmpty)
      assert(
        res.value == List(
          "internationalization"
        )
      )
    }

    "parse no global criteria if empty" in {
      val input       = "global_criteria:"
      val (res, rest) = parser.parse(input)
      assert(rest.isEmpty)
      assert(res.value.isEmpty)
    }
  }
}
