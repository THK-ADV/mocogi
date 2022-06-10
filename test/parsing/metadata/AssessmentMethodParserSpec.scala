package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.metadata.AssessmentMethodParser.{assessmentMethodFileParser, assessmentMethodParser}
import parsing.types.AssessmentMethod
import parsing.{ParserSpecHelper, withResFile}

class AssessmentMethodParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Assessment Method Parser" should {
    "parse assignment method file" when {
      "parse a single assessment method" in {
        val input =
          """written-exam:
            |  de_label: Klausurarbeiten""".stripMargin
        val (res, rest) = assessmentMethodFileParser.parse(input)
        assert(
          res.value == List(AssessmentMethod("written-exam", "Klausurarbeiten"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple assessment methods" in {
        val input =
          """project:
            |  de_label: Projektarbeit
            |
            |project-documentation:
            |  de_label: Projektdokumentation
            |
            |portfolio:
            |  de_label: Lernportfolio
            |
            |practical-report:
            |  de_label: Praktikumsbericht""".stripMargin
        val (res, rest) = assessmentMethodFileParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethod("project", "Projektarbeit"),
            AssessmentMethod("project-documentation", "Projektdokumentation"),
            AssessmentMethod("portfolio", "Lernportfolio"),
            AssessmentMethod("practical-report", "Praktikumsbericht")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all assessment methods in assessment.yaml" in {
        val (res, rest) =
          withResFile("assessment.yaml")(assessmentMethodFileParser.parse)
        assert(
          res.value == List(
            AssessmentMethod("written-exam", "Klausurarbeiten"),
            AssessmentMethod(
              "written-exam-answer-choice-method",
              "Schriftliche Prüfungen im Antwortwahlverfahren"
            ),
            AssessmentMethod("oral-exams", "Mündliche Prüfungen"),
            AssessmentMethod("presentation", "Präsentation"),
            AssessmentMethod("home-assignment", "Hausarbeit"),
            AssessmentMethod("project", "Projektarbeit"),
            AssessmentMethod("project-documentation", "Projektdokumentation"),
            AssessmentMethod("portfolio", "Lernportfolio"),
            AssessmentMethod("practical-report", "Praktikumsbericht"),
            AssessmentMethod(
              "practical-semester-report",
              "Praxissemesterbericht"
            ),
            AssessmentMethod("practical", "Praktikum"),
            AssessmentMethod("test", "Schriftlicher Test"),
            AssessmentMethod("thesis", "Schriftliche Ausarbeitung")
          )
        )
        assert(rest.isEmpty)
      }
    }

    "parse assessment method" when {
      "return a single assessment method" in {
        val input = "assessment-methods:assessment.written-exam\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethod("written-exam", "Klausurarbeiten")
          )
        )
        assert(rest.isEmpty)
      }

      "return a single assessment method ignoring random whitespaces" in {
        val input = "assessment-methods: assessment.written-exam\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethod("written-exam", "Klausurarbeiten")
          )
        )
        assert(rest.isEmpty)
      }

      "return 2 assessment methods seperated by dashes" in {
        val input =
          """assessment-methods:
              |-assessment.written-exam
              |-assessment.presentation
              |""".stripMargin
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethod("written-exam", "Klausurarbeiten"),
            AssessmentMethod("presentation", "Präsentation")
          )
        )
        assert(rest.isEmpty)
      }

      "return 2 assessment methods seperated by dashes ignoring random whitespace" in {
        val input =
          """assessment-methods:
              | - assessment.written-exam
              | - assessment.presentation
              |""".stripMargin
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethod("written-exam", "Klausurarbeiten"),
            AssessmentMethod("presentation", "Präsentation")
          )
        )
        assert(rest.isEmpty)
      }

      "return 2 assessment methods seperated by dashes ignoring random whitespace with a remaining inout" in {
        val input =
          """assessment-methods:
              | - assessment.written-exam
              | - assessment.presentation
              | abc""".stripMargin
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethod("written-exam", "Klausurarbeiten"),
            AssessmentMethod("presentation", "Präsentation")
          )
        )
        assert(rest == " abc")
      }
    }
  }
}
