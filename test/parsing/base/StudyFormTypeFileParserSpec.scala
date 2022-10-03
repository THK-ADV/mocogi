package parsing.base

import basedata.StudyFormType
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.base.StudyFormTypeFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

final class StudyFormTypeFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A StudyFormType File Parser" should {
    "parse a single study form type" in {
      val input =
        """full:
          |  de_label: Vollzeitstudium
          |  en_label: Full time study""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(
        res.value == List(
          StudyFormType("full", "Vollzeitstudium", "Full time study")
        )
      )
      assert(rest.isEmpty)
    }

    "parse multiple study form types" in {
      val input =
        """full:
          |  de_label: Vollzeitstudium
          |  en_label: Full time study
          |part:
          |  de_label: Teilzeitstudium
          |  en_label: Part time study""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(
        res.value == List(
          StudyFormType("full", "Vollzeitstudium", "Full time study"),
          StudyFormType("part", "Teilzeitstudium", "Part time study")
        )
      )
      assert(rest.isEmpty)
    }

    "parse study_form.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/study_form.yaml")(fileParser.parse)
      assert(
        res.value == List(
          StudyFormType("full", "Vollzeitstudium", "Full time study"),
          StudyFormType("part", "Teilzeitstudium", "Part time study"),
          StudyFormType("dual", "Duales Studium", "dual study"),
          StudyFormType(
            "bbw",
            "Berufsbegleitendes Weiterbildungsstudium",
            "--"
          ),
          StudyFormType("bbs", "Berufsbegleitender Studiengang", "--")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
