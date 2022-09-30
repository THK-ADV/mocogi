package parsing.base

import basedata.AssessmentMethod
import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.base.AssessmentMethodFileParser
import parsing.{ParserSpecHelper, withFile0}

class AssessmentMethodFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser =
    app.injector.instanceOf(classOf[AssessmentMethodFileParser]).fileParser

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
        withFile0("test/parsing/res/assessment.yaml")(
          parser.parse
        )
      assert(
        res.value == List(
          AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
          AssessmentMethod(
            "written-exam-answer-choice-method",
            "Schriftliche Prüfungen im Antwortwahlverfahren",
            "--"
          ),
          AssessmentMethod("oral-exams", "Mündliche Prüfungen", "--"),
          AssessmentMethod("presentation", "Präsentation", "--"),
          AssessmentMethod("home-assignment", "Hausarbeit", "--"),
          AssessmentMethod("project", "Projektarbeit", "--"),
          AssessmentMethod(
            "project-documentation",
            "Projektdokumentation",
            "--"
          ),
          AssessmentMethod("portfolio", "Lernportfolio", "--"),
          AssessmentMethod("practical-report", "Praktikumsbericht", "--"),
          AssessmentMethod(
            "practical-semester-report",
            "Praxissemesterbericht",
            "--"
          ),
          AssessmentMethod("practical", "Praktikum", "--"),
          AssessmentMethod("test", "Schriftlicher Test", "--"),
          AssessmentMethod("thesis", "Schriftliche Ausarbeitung", "--")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
