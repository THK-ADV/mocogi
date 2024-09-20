package parsing.metadata

import helper.FakeAssessmentMethod
import models.core.AssessmentMethod
import models.ModuleAssessmentMethodEntryProtocol
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.metadata.ModuleAssessmentMethodParser._
import parsing.types.ModuleAssessmentMethodEntry
import parsing.ParserSpecHelper

class ModuleAssessmentMethodParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeAssessmentMethod {

  "A Assessment Method Parser" should {
    "dont parse assessment methods if there are none" in {
      val input         = "foo: bar";
      val (res1, rest1) = mandatoryParser.parse(input)
      assert(res1.value.isEmpty)
      assert(rest1 == "foo: bar")

      val (res2, rest2) = electiveParser.parse(input)
      assert(res2.value.isEmpty)
      assert(rest2 == "foo: bar")

      val (res3, rest3) = mandatoryParserRaw.parse(input)
      assert(res3.value.isEmpty)
      assert(rest3 == "foo: bar")

      val (res4, rest4) = electiveParserRaw.parse(input)
      assert(res4.value.isEmpty)
      assert(rest4 == "foo: bar")

      val (res5, rest5) = raw.parse(input)
      assert(res5.value.optional.isEmpty)
      assert(res5.value.mandatory.isEmpty)
      assert(rest5 == "foo: bar")

      val (res6, rest6) = parser.parse(input)
      assert(res6.value.optional.isEmpty)
      assert(res6.value.mandatory.isEmpty)
      assert(rest6 == "foo: bar")
    }

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
