package parsing.metadata

import helper.{FakeApplication, FakeAssessmentMethod}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper
import parsing.types.{AssessmentMethod, AssessmentMethodPercentage}

class AssessmentMethodParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeAssessmentMethod {

  val parser = app.injector
    .instanceOf(classOf[AssessmentMethodParser])
    .parser

  "A Assessment Method Parser" should {
    "return a single assessment method without percentage info" in {
      val input = "assessment-methods:assessment.written-exam\n"
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None
          )
        )
      )
      assert(rest.isEmpty)
    }

    "return a single assessment method with percentage info without spaces but with percent sign" in {
      val input = "assessment-methods:assessment.written-exam (100%)\n"
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(100)
          )
        )
      )
      assert(rest.isEmpty)
    }

    "return a single assessment method with percentage info with spaces and percent sign" in {
      val input = "assessment-methods:assessment.written-exam (100 %)\n"
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(100)
          )
        )
      )
      assert(rest.isEmpty)
    }

    "return a single assessment method with percentage info without spaces and percent sign" in {
      val input = "assessment-methods:assessment.written-exam (100)\n"
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(100)
          )
        )
      )
      assert(rest.isEmpty)
    }

    "fail if a single assessment method with percentage info is higher than 100 %" in {
      val input = "assessment-methods:assessment.written-exam (110 %)\n"
      val (res, rest) = parser.parse(input)
      val e = res.left.value
      assert(
        e.expected == "percentage of all assessment methods to be 100.0 %, but was 110.0 %"
      )
      assert(e.found == input)
      assert(rest == input)
    }

    "return a single assessment method ignoring random whitespaces" in {
      val input = "assessment-methods: assessment.written-exam\n"
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
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
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None
          ),
          AssessmentMethodPercentage(
            AssessmentMethod("presentation", "Präsentation", "--"),
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
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None
          ),
          AssessmentMethodPercentage(
            AssessmentMethod("presentation", "Präsentation", "--"),
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
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            Some(70)
          ),
          AssessmentMethodPercentage(
            AssessmentMethod("presentation", "Präsentation", "--"),
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
      val (res, rest) = parser.parse(input)
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
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          AssessmentMethodPercentage(
            AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
            None
          ),
          AssessmentMethodPercentage(
            AssessmentMethod("presentation", "Präsentation", "--"),
            None
          )
        )
      )
      assert(rest == " abc")
    }
  }
}
