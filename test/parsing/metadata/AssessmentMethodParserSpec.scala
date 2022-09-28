package parsing.metadata

import helper.FakeAssessmentMethod
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.AssessmentMethodParser._
import parsing.types.{AssessmentMethod, AssessmentMethodEntry}

class AssessmentMethodParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeAssessmentMethod {

  "A Assessment Method Parser" should {
    "parse a single assessment method" in {
      val input = """assessment_methods_mandatory:
          |  - method: assessment.written-exam""".stripMargin
      val (res, rest) = assessmentMethodsMandatoryParser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None,
            Nil
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single assessment method with percentage" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    percentage: 100""".stripMargin
      val (res, rest) = assessmentMethodsMandatoryParser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(100.0),
            Nil
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single assessment method with percentage and precondition" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    percentage: 100
          |    precondition:
          |      - assessment.practical""".stripMargin
      val (res, rest) = assessmentMethodsMandatoryParser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(100.0),
            List(AssessmentMethod("practical", "Praktikum", "--"))
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single assessment method with precondition but without percentage" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    precondition:
          |      - assessment.practical""".stripMargin
      val (res, rest) = assessmentMethodsMandatoryParser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None,
            List(AssessmentMethod("practical", "Praktikum", "--"))
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse multiple assessment methods with different combinations" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    percentage: 70
          |    precondition:
          |      - assessment.practical
          |  - method: assessment.project
          |    percentage: 30
          |  - method: assessment.oral-exams""".stripMargin
      val (res, rest) = assessmentMethodsMandatoryParser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(70.0),
            List(AssessmentMethod("practical", "Praktikum", "--"))
          ),
          AssessmentMethodEntry(
            AssessmentMethod("project", "Projektarbeit", "--"),
            Some(30.0),
            Nil
          ),
          AssessmentMethodEntry(
            AssessmentMethod("oral-exams", "Mündliche Prüfungen", "--"),
            None,
            Nil
          )
        )
      )
      assert(rest.isEmpty)
    }

    "fail parsing a single assessment method if method key is missing" in {
      val input =
        """assessment_methods_mandatory:
          |  - assessment.written-exam""".stripMargin
      val (res, rest) = assessmentMethodsMandatoryParser.parse(input)
      assert(res.value.isEmpty)
      assert(rest == "- assessment.written-exam")
    }
  }
}
