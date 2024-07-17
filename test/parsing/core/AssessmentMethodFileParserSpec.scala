package parsing.core

import models.core.AssessmentMethod
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{ParserSpecHelper, withFile0}

class AssessmentMethodFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  val parser = AssessmentMethodFileParser.parser()

  "A Assessment Method File Parser" should {
    "parse a single assessment method" in {
      val input =
        """written-exam:
          |  de_label: Klausurarbeiten
          |  en_label: written exam""".stripMargin

      val (res, rest) = parser.parse(input)
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
      val (res, rest) = parser.parse(input)
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
        withFile0("test/parsing/res/assessment.yaml")(parser.parse)
      val ids = List(
        "tbd",
        "written-exam",
        "written-exam-answer-choice-method",
        "oral-exams",
        "presentation",
        "home-assignment",
        "project",
        "project-documentation",
        "portfolio",
        "practical-report",
        "practical-semester-report",
        "practical",
        "test",
        "thesis",
        "abstract",
        "e-assessment",
        "single-choice",
        "multiple-choice",
        "certificate",
        "continous-report",
        "attendance",
        "verbal-cooperation"
      )
      res.value.zip(ids).foreach { case (am, id) =>
        assert(am.id == id)
        assert(am.deLabel.nonEmpty)
        assert(am.enLabel.isEmpty)
      }
      assert(rest.isEmpty)
    }
  }
}
