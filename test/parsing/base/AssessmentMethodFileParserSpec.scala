package parsing.base

import basedata.AssessmentMethod
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.base.AssessmentMethodFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

class AssessmentMethodFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Assessment Method File Parser" should {
    "parse a single assessment method" in {
      val input =
        """written-exam:
          |  de_label: Klausurarbeiten
          |  en_label: written exam""".stripMargin

      val (res, rest) = fileParser.parse(input)
      assert(
        res.value == List(
          AssessmentMethod("written-exam", "Klausurarbeiten", "written exam")
        )
      )
      assert(rest.isEmpty)
    }

    "parse multiple assessment methods" in {
      val input =
        """project:
          |  de_label: Projektarbeit
          |  en_label: project
          |
          |project-documentation:
          |  de_label: Projektdokumentation
          |  en_label: project documentation
          |
          |portfolio:
          |  de_label: Lernportfolio
          |  en_label: portfolio
          |
          |practical-report:
          |  de_label: Praktikumsbericht
          |  en_label: labwork report""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(
        res.value == List(
          AssessmentMethod("project", "Projektarbeit", "project"),
          AssessmentMethod(
            "project-documentation",
            "Projektdokumentation",
            "project documentation"
          ),
          AssessmentMethod("portfolio", "Lernportfolio", "portfolio"),
          AssessmentMethod(
            "practical-report",
            "Praktikumsbericht",
            "labwork report"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse all assessment methods in assessment.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/assessment.yaml")(
          fileParser.parse
        )
      assert(res.value.size == 22)
      assert(rest.isEmpty)
    }
  }
}
