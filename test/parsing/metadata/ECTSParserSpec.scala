package parsing.metadata

import basedata.FocusAreaPreview
import helper.FakeFocusAreas
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ECTSParser._
import parsing.types.ECTSFocusAreaContribution

final class ECTSParserSpec
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
          |      desc:
          |    acs:
          |      num: 6
          |      desc: |
          |        Text1
          |        Text2""".stripMargin
      val (res, rest) =
        ectsContributionsToFocusAreasParser.parse(input)
      assert(
        res.value == List(
          ECTSFocusAreaContribution(FocusAreaPreview("gak"), 0, ""),
          ECTSFocusAreaContribution(FocusAreaPreview("acs"), 6, "Text1\nText2\n")
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
          |      desc: |
          |        Text1
          |        Text2
          |    acs:
          |      num: 6
          |      desc: |
          |        Text1
          |        Text2""".stripMargin
      val (res2, rest2) = ectsParser.parse(complexInput)
      assert(
        res2.value.value == List(
          ECTSFocusAreaContribution(FocusAreaPreview("gak"), 4, "Text1\nText2\n"),
          ECTSFocusAreaContribution(FocusAreaPreview("acs"), 6, "Text1\nText2\n")
        )
      )
      assert(rest2.isEmpty)
    }
  }
}
