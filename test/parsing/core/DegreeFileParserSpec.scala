package parsing.core

import helper.FakeDegrees
import models.core.Degree
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.withFile0
import parsing.ParserSpecHelper

final class DegreeFileParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues with FakeDegrees {

  val parser = DegreeFileParser.parser()

  "A Degree File Parser" should {
    "parse a single degree" in {
      val input =
        """bsc:
          |  de_label: B.Sc.
          |  de_desc: Bachelor of Science
          |  en_label: B.Sc.
          |  en_desc: Bachelor of Science""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          Degree(
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

    "parse multiple degrees" in {
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
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          Degree(
            "bsc",
            "B.Sc.",
            "Bachelor of Science",
            "B.Sc.",
            "Bachelor of Science"
          ),
          Degree(
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
          parser.parse
        )
      assert(res.value == List(bsc, msc, beng, meng))
      assert(rest.isEmpty)
    }
  }
}
