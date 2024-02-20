package parsing.metadata

import helper.FakeAssessmentMethod
import models.ModuleAssessmentMethodEntryProtocol
import models.core.AssessmentMethod
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleAssessmentMethodParser._
import parsing.types.ModuleAssessmentMethodEntry

class ModuleAssessmentMethodParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeAssessmentMethod {

  "A Assessment Method Parser" should {
    "parse a single assessment method" in {
      val input = """assessment_methods_mandatory:
          |  - method: assessment.written-exam""".stripMargin
      val (res, rest) = mandatoryParser.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None,
            Nil
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single assessment method raw" in {
      val input = """assessment_methods_mandatory:
          |  - method: assessment.written-exam""".stripMargin
      val (res, rest) = mandatoryParserRaw.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntryProtocol(
            "written-exam",
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
      val (res, rest) = mandatoryParser.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(100.0),
            Nil
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single assessment method with percentage raw" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    percentage: 100""".stripMargin
      val (res, rest) = mandatoryParserRaw.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntryProtocol(
            "written-exam",
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
      val (res, rest) = mandatoryParser.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(100.0),
            List(AssessmentMethod("practical", "Praktikum", "--"))
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single assessment method with percentage and precondition raw" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    percentage: 100
          |    precondition:
          |      - assessment.practical""".stripMargin
      val (res, rest) = mandatoryParserRaw.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntryProtocol(
            "written-exam",
            Some(100.0),
            List("practical")
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
      val (res, rest) = mandatoryParser.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None,
            List(AssessmentMethod("practical", "Praktikum", "--"))
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single assessment method with precondition but without percentage raw" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    precondition:
          |      - assessment.practical""".stripMargin
      val (res, rest) = mandatoryParserRaw.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntryProtocol(
            "written-exam",
            None,
            List("practical")
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
      val (res, rest) = mandatoryParser.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntry(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(70.0),
            List(AssessmentMethod("practical", "Praktikum", "--"))
          ),
          ModuleAssessmentMethodEntry(
            AssessmentMethod("project", "Projektarbeit", "--"),
            Some(30.0),
            Nil
          ),
          ModuleAssessmentMethodEntry(
            AssessmentMethod("oral-exams", "Mündliche Prüfungen", "--"),
            None,
            Nil
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse multiple assessment methods with different combinations raw" in {
      val input =
        """assessment_methods_mandatory:
          |  - method: assessment.written-exam
          |    percentage: 70
          |    precondition:
          |      - assessment.practical
          |  - method: assessment.project
          |    percentage: 30
          |  - method: assessment.oral-exams""".stripMargin
      val (res, rest) = mandatoryParserRaw.parse(input)
      assert(
        res.value == List(
          ModuleAssessmentMethodEntryProtocol(
            "written-exam",
            Some(70.0),
            List("practical")
          ),
          ModuleAssessmentMethodEntryProtocol(
            "project",
            Some(30.0),
            Nil
          ),
          ModuleAssessmentMethodEntryProtocol(
            "oral-exams",
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
      val (res, rest) = mandatoryParser.parse(input)
      assert(res.value.isEmpty)
      assert(rest == "- assessment.written-exam")
    }
  }
}
