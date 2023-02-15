package parsing.core

import helper.FakeGrades
import models.core.Grade
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.core.GradeFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

final class GradeFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeGrades {

  "A Grade File Parser" should {
    "parse a single grade" in {
      val input =
        """bsc:
          |  de_label: B.Sc.
          |  de_desc: Bachelor of Science
          |  en_label: B.Sc.
          |  en_desc: Bachelor of Science""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(
        res.value == List(
          Grade(
            "bsc",
            "B.Sc.",
            "Bachelor of Science",
            "B.Sc.",
            "Bachelor of Science"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse multiple grades" in {
      val input =
        """bsc:
          |  de_label: B.Sc.
          |  de_desc: Bachelor of Science
          |  en_label: B.Sc.
          |  en_desc: Bachelor of Science
          |
          |beng:
          |  de_label: B.Eng.
          |  de_desc: Bachelor of Engineering
          |  en_label: B.Eng.
          |  en_desc: Bachelor of Engineering""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(
        res.value == List(
          Grade(
            "bsc",
            "B.Sc.",
            "Bachelor of Science",
            "B.Sc.",
            "Bachelor of Science"
          ),
          Grade(
            "beng",
            "B.Eng.",
            "Bachelor of Engineering",
            "B.Eng.",
            "Bachelor of Engineering"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse all in grades.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/grades.yaml")(
          fileParser.parse
        )
      assert(res.value == List(bsc, msc, beng, meng))
      assert(rest.isEmpty)
    }
  }
}
