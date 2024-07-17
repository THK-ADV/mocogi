package parsing.core

import models.core.Faculty
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{ParserSpecHelper, withFile0}

final class FacultyFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  val parser = FacultyFileParser.parser()

  "A Faculty File Parser" should {
    "parse a single faculty" in {
      val input =
        """f01:
          |  de_label: a
          |  en_label: b""".stripMargin
      val (res, rest) = parser.parse(input)
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
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(Faculty("f01", "a", "b"), Faculty("f02", "c", "d"))
      )
      assert(rest.isEmpty)
    }

    "parse all faculties in faculty.yaml" in {
      val (res, rest) = withFile0("test/parsing/res/faculty.yaml")(parser.parse)
      val ids = List(
        "f01",
        "f02",
        "f03",
        "f04",
        "f05",
        "f06",
        "f07",
        "f08",
        "f09",
        "f10",
        "f11",
        "f12"
      )
      res.value.zip(ids).foreach { case (faculty, id) =>
        assert(faculty.id == id)
        assert(faculty.deLabel.nonEmpty)
        assert(faculty.enLabel.nonEmpty)
      }
      assert(rest.isEmpty)
    }
  }
}
