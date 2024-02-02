package parsing.metadata

import helper.{FakePOs, FakeSpecializations}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.ParserSpecHelper
import parsing.metadata.POParser._
import parsing.types.{POMandatory, ParsedPOOptional}

import java.util.UUID

class POParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with OptionValues
    with FakePOs
    with FakeSpecializations {

  "A PO Parser" should {
    "parse a study program with no specialization" in {
      val input = "- study_program: study_program.wi1"
      val (res1, rest1) = studyProgramParser.parse(input)
      assert(rest1.isEmpty)
      assert(res1.value._1.id == "wi1")
      assert(res1.value._2.isEmpty)
    }

    "parse a study program with specialization" in {
      val input = "- study_program: study_program.wi1.wi1_vi"
      val (res1, rest1) = studyProgramParser.parse(input)
      assert(rest1.isEmpty)
      assert(res1.value._1.id == "wi1")
      assert(res1.value._2.value.id == "wi1_vi")
    }

    "parse a study program with no specialization if specialization is not found in po" in {
      val input = "- study_program: study_program.wi1.wi1_abc"
      val (res1, rest1) = studyProgramParser.parse(input)
      assert(rest1 == input)
      assert(
        res1.left.value.expected == "wi1_vi or mi1_az or itm1_ab or inf1_sc"
      )
      assert(res1.left.value.found == "wi1_abc")
    }

    "parse a single mandatory po with no specialization" in {
      val input =
        """po_mandatory:
          |  - study_program: study_program.wi1
          |    recommended_semester:
          |      - 3
          |      - 4
          |    recommended_semester_part_time:
          |      - 1
          |      - 2""".stripMargin
      val (res, rest) = mandatoryPOParser.parse(input)
      assert(
        res.value == List(
          POMandatory(wi1, None, List(3, 4))
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single mandatory po with specialization" in {
      val input =
        """po_mandatory:
          |  - study_program: study_program.wi1.wi1_vi
          |    recommended_semester:
          |      - 3
          |      - 4
          |    recommended_semester_part_time:
          |      - 1
          |      - 2""".stripMargin
      val (res, rest) = mandatoryPOParser.parse(input)
      assert(
        res.value == List(
          POMandatory(wi1, Some(vi), List(3, 4))
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single mandatory po where recommended semester is missing" in {
      val input =
        """po_mandatory:
          |  - study_program: study_program.wi1""".stripMargin
      val (res, rest) = mandatoryPOParser.parse(input)
      assert(
        res.value == List(POMandatory(wi1, None, Nil))
      )
      assert(rest.isEmpty)
    }

    "parse a single mandatory po where recommended semester part time is missing" in {
      val input =
        """po_mandatory:
          |  - study_program: study_program.wi1
          |    recommended_semester:
          |      - 3
          |      - 4""".stripMargin
      val (res, rest) = mandatoryPOParser.parse(input)
      assert(
        res.value == List(POMandatory(wi1, None, List(3, 4)))
      )
      assert(rest.isEmpty)
    }

    "parse many mandatory pos" in {
      val input =
        """po_mandatory:
          |  - study_program: study_program.wi1
          |    recommended_semester:
          |      - 3
          |      - 4
          |    recommended_semester_part_time:
          |      - 1
          |      - 2
          |  - study_program: study_program.mi1
          |    recommended_semester: 5""".stripMargin
      val (res, rest) = mandatoryPOParser.parse(input)
      assert(
        res.value == List(
          POMandatory(wi1, None, List(3, 4)),
          POMandatory(mi1, None, List(5))
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single optional po with no specialization" in {
      val m1 = UUID.randomUUID
      val input =
        s"""po_optional:
          |  - study_program: study_program.wi1
          |    instance_of: module.$m1
          |    part_of_catalog: false
          |    recommended_semester: 3""".stripMargin
      val (res, rest) = optionalPOParser.parse(input)
      assert(
        res.value == List(
          ParsedPOOptional(wi1, None, m1, partOfCatalog = false, List(3))
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single optional po with specialization" in {
      val m1 = UUID.randomUUID
      val input =
        s"""po_optional:
          |  - study_program: study_program.wi1.wi1_vi
          |    instance_of: module.$m1
          |    part_of_catalog: false
          |    recommended_semester: 3""".stripMargin
      val (res, rest) = optionalPOParser.parse(input)
      assert(
        res.value == List(
          ParsedPOOptional(wi1, Some(vi), m1, partOfCatalog = false, List(3))
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single optional po where recommended semester is missing" in {
      val m1 = UUID.randomUUID
      val input =
        s"""po_optional:
           |  - study_program: study_program.wi1
           |    instance_of: module.$m1
           |    part_of_catalog: false""".stripMargin
      val (res, rest) = optionalPOParser.parse(input)
      assert(
        res.value == List(
          ParsedPOOptional(wi1, None, m1, partOfCatalog = false, Nil)
        )
      )
      assert(rest.isEmpty)
    }

    "parse many optional pos" in {
      val m1 = UUID.randomUUID
      val m2 = UUID.randomUUID
      val input =
        s"""po_optional:
          |  - study_program: study_program.wi1
          |    instance_of: module.$m1
          |    part_of_catalog: false
          |    recommended_semester: 3
          |  - study_program: study_program.inf1
          |    instance_of: module.$m2
          |    part_of_catalog: true
          |    recommended_semester:
          |      - 3
          |      - 1""".stripMargin
      val (res, rest) = optionalPOParser.parse(input)
      assert(
        res.value == List(
          ParsedPOOptional(wi1, None, m1, partOfCatalog = false, List(3)),
          ParsedPOOptional(inf1, None, m2, partOfCatalog = true, List(3, 1))
        )
      )
      assert(rest.isEmpty)
    }
  }
}
