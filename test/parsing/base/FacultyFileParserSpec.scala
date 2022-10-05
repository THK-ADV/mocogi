package parsing.base

import basedata.Faculty
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.base.FacultyFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

final class FacultyFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Faculty File Parser" should {
    "parse a single faculty" in {
      val input =
        """f01:
          |  de_label: a
          |  en_label: b""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(res.value == List(Faculty("f01", "a", "b")))
      assert(rest.isEmpty)
    }

    "parse multiple faculties" in {
      val input =
        """f01:
          |  de_label: a
          |  en_label: b
          |
          |f02:
          |  de_label: c
          |  en_label: d""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(
        res.value == List(Faculty("f01", "a", "b"), Faculty("f02", "c", "d"))
      )
      assert(rest.isEmpty)
    }

    "parse all faculties in faculty.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/faculty.yaml")(
          fileParser.parse
        )
      assert(res.value.size == 12)
      assert(rest.isEmpty)
    }
  }
}
