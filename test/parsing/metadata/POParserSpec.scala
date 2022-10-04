package parsing.metadata

import helper.FakePOs
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.POParser._
import parsing.types.{POMandatory, ParsedPOOptional}

class POParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakePOs {

  "A PO Parser" should {
    "parse a single mandatory po" in {
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
          POMandatory(wi1, List(3, 4), List(1, 2))
        )
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
        res.value == List(POMandatory(wi1, List(3, 4), Nil))
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
          POMandatory(wi1, List(3, 4), List(1, 2)),
          POMandatory(mi1, List(5), Nil)
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single optional po" in {
      val input =
        """po_optional:
          |  - study_program: study_program.wi1
          |    instance_of: module.wpf
          |    part_of_catalog: false
          |    recommended_semester: 3""".stripMargin
      val (res, rest) = optionalPOParser.parse(input)
      assert(
        res.value == List(
          ParsedPOOptional(wi1, "wpf", partOfCatalog = false, List(3))
        )
      )
      assert(rest.isEmpty)
    }

    "parse many optional pos" in {
      val input =
        """po_optional:
          |  - study_program: study_program.wi1
          |    instance_of: module.wpf
          |    part_of_catalog: false
          |    recommended_semester: 3
          |  - study_program: study_program.inf1
          |    instance_of: module.wpf
          |    part_of_catalog: true
          |    recommended_semester:
          |      - 3
          |      - 1""".stripMargin
      val (res, rest) = optionalPOParser.parse(input)
      assert(
        res.value == List(
          ParsedPOOptional(wi1, "wpf", partOfCatalog = false, List(3)),
          ParsedPOOptional(inf1, "wpf", partOfCatalog = true, List(3, 1))
        )
      )
      assert(rest.isEmpty)
    }
  }
}
