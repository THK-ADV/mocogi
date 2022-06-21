package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.metadata.AssessmentMethodParser.{
  assessmentMethodFileParser,
  assessmentMethodParser
}
import parsing.types.{AssessmentMethod, AssessmentMethodPercentage}
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
      "return a single assessment method without percentage info" in {
        val input = "assessment-methods:assessment.written-exam\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              None
            )
          )
        )
        assert(rest.isEmpty)
      }

      "return a single assessment method with percentage info without spaces but with percent sign" in {
        val input = "assessment-methods:assessment.written-exam (100%)\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              Some(100)
            )
          )
        )
        assert(rest.isEmpty)
      }

      "return a single assessment method with percentage info with spaces and percent sign" in {
        val input = "assessment-methods:assessment.written-exam (100 %)\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              Some(100)
            )
          )
        )
        assert(rest.isEmpty)
      }

      "return a single assessment method with percentage info without additional syntax" in {
        val input = "assessment-methods:assessment.written-exam 100\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              Some(100)
            )
          )
        )
        assert(rest.isEmpty)
      }

      "return a single assessment method with percentage info without spaces and percent sign" in {
        val input = "assessment-methods:assessment.written-exam (100)\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              Some(100)
            )
          )
        )
        assert(rest.isEmpty)
      }

      "fail if a single assessment method with percentage info is higher than 100 %" in {
        val input = "assessment-methods:assessment.written-exam (110 %)\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        val e = res.left.value
        assert(
          e.expected == "percentage of all assessment methods to be 100.0 %, but was 110.0 %"
        )
        assert(e.found == input)
        assert(rest == input)
      }

      "return a single assessment method ignoring random whitespaces" in {
        val input = "assessment-methods: assessment.written-exam\n"
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              None
            )
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
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              None
            ),
            AssessmentMethodPercentage(
              AssessmentMethod("presentation", "Präsentation"),
              None
            )
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
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              None
            ),
            AssessmentMethodPercentage(
              AssessmentMethod("presentation", "Präsentation"),
              None
            )
          )
        )
        assert(rest.isEmpty)
      }

      "return 2 assessment methods seperated by dashes ignoring random whitespace with percent info" in {
        val input =
          """assessment-methods:
              | - assessment.written-exam (70%)
              | - assessment.presentation (30%)
              |""".stripMargin
        val (res, rest) = assessmentMethodParser.parse(input)
        assert(
          res.value == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              Some(70)
            ),
            AssessmentMethodPercentage(
              AssessmentMethod("presentation", "Präsentation"),
              Some(30)
            )
          )
        )
        assert(rest.isEmpty)
      }

      "fail if 2 assessment methods with percentage info are higher than 100 %" in {
        val input =
          """assessment-methods:
              | - assessment.written-exam (80%)
              | - assessment.presentation (40%)
              |""".stripMargin
        val (res, rest) = assessmentMethodParser.parse(input)
        val e = res.left.value
        assert(
          e.expected == "percentage of all assessment methods to be 100.0 %, but was 120.0 %"
        )
        assert(e.found == input)
        assert(rest == input)
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
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten"),
              None
            ),
            AssessmentMethodPercentage(
              AssessmentMethod("presentation", "Präsentation"),
              None
            )
          )
        )
        assert(rest == " abc")
      }
    }
  }
}
