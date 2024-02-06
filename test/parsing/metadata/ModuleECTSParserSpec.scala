package parsing.metadata

import helper.FakeFocusAreas
import models.core.FocusAreaID
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleECTSParser._
import parsing.types.ModuleECTSFocusAreaContribution

final class ModuleECTSParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeFocusAreas {

  "A ECTS Parser" should {
    "parse a simple ects value" in {
      val (res1, rest1) = ectsValueParser.parse("ects: 5")
      assert(res1.value == 5)
      assert(rest1.isEmpty)
      val (res2, rest2) = ectsValueParser.parse("ects: -5")
      assert(res2.value == -5)
      assert(rest2.isEmpty)
      val (res3, rest3) = ectsValueParser.parse("ects: 2.5")
      assert(res3.value == 2.5)
      assert(rest3.isEmpty)
    }

    "parse ects with contributions to focus areas" in {
      val input =
        """ects:
          |  contributions_to_focus_areas:
          |    gak:
          |      num: 0
          |      de_desc:
          |    acs:
          |      num: 6
          |      de_desc: |
          |        Text1
          |        Text2""".stripMargin
      val (res, rest) =
        ectsContributionsToFocusAreasParser.parse(input)
      assert(
        res.value.head == ModuleECTSFocusAreaContribution(
          FocusAreaID("gak"),
          0,
          "",
          ""
        )
      )
      assert(
        res.value(1) == ModuleECTSFocusAreaContribution(
          FocusAreaID("acs"),
          6,
          "Text1\nText2\n",
          ""
        )
      )
      assert(rest.isEmpty)
    }

    "parse ects with contributions to focus areas with de and en desc set or not" in {
      val input =
        """ects:
          |  contributions_to_focus_areas:
          |    gak:
          |      num: 0
          |      de_desc: a
          |    acs:
          |      num: 6
          |      de_desc: |
          |        a
          |        b
          |      en_desc: c  """.stripMargin
      val (res, rest) =
        ectsContributionsToFocusAreasParser.parse(input)
      assert(
        res.value.head == ModuleECTSFocusAreaContribution(
          FocusAreaID("gak"),
          0,
          "a",
          ""
        )
      )
      assert(
        res.value(1) == ModuleECTSFocusAreaContribution(
          FocusAreaID("acs"),
          6,
          "a\nb\n",
          "c"
        )
      )
      assert(rest.isEmpty)
    }

    "parse a simple or a complex ects value" in {
      val simpleInput = "ects: 5"
      val (res1, rest1) = ectsParser.parse(simpleInput)
      assert(res1.value.left.value == 5)
      assert(rest1.isEmpty)

      val complexInput =
        """ects:
          |  contributions_to_focus_areas:
          |    gak:
          |      num: 4
          |      de_desc: |
          |        Text1
          |        Text2
          |    acs:
          |      num: 6
          |      de_desc: |
          |        Text1
          |        Text2""".stripMargin
      val (res2, rest2) = ectsParser.parse(complexInput)
      assert(
        res2.value.value == List(
          ModuleECTSFocusAreaContribution(
            FocusAreaID("gak"),
            4,
            "Text1\nText2\n",
            ""
          ),
          ModuleECTSFocusAreaContribution(
            FocusAreaID("acs"),
            6,
            "Text1\nText2\n",
            ""
          )
        )
      )
      assert(rest2.isEmpty)
    }
  }
}
