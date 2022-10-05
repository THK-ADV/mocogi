package parsing.base

import helper.FakeStudyProgramPreviews
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.base.FocusAreaFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

final class FocusAreaFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeStudyProgramPreviews {
  "A Focus Area File Parser" should {
    "parse a single focus area" in {
      val input =
        """# comment
          |ar:
          |  program: program.inf_itm
          |  de_label: ok
          |  en_label: ok
          |  de_desc: >
          |    abc
          |
          |    def
          |  en_desc: >
          |    ok
          |
          |    ko""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val fa = res.value.head
      assert(fa.abbrev == "ar")
      assert(fa.program == itm)
      assert(fa.deLabel == "ok")
      assert(fa.enLabel == "ok")
      assert(fa.deDesc == "abc\ndef\n")
      assert(fa.enDesc == "ok\nko\n")
      assert(rest.isEmpty)
    }

    "parse multiple focus areas" in {
      val input =
        """# comment
          |ar:
          |  program: program.inf_itm
          |  de_label: ok
          |  en_label: ok
          |pup:
          |  program: program.inf_itm
          |  de_label: ok
          |
          |# a
          |bui:
          |  program: program.inf_itm
          |  de_label: ok
          |  de_desc: test""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val fa1 = res.value.head
      assert(fa1.abbrev == "ar")
      assert(fa1.program == itm)
      assert(fa1.deLabel == "ok")
      assert(fa1.enLabel == "ok")
      assert(fa1.deDesc.isEmpty)
      assert(fa1.enDesc.isEmpty)

      val fa2 = res.value(1)
      assert(fa2.abbrev == "pup")
      assert(fa2.program == itm)
      assert(fa2.deLabel == "ok")
      assert(fa2.enLabel.isEmpty)
      assert(fa2.deDesc.isEmpty)
      assert(fa2.enDesc.isEmpty)

      val fa3 = res.value(2)
      assert(fa3.abbrev == "bui")
      assert(fa3.program == itm)
      assert(fa3.deLabel == "ok")
      assert(fa3.enLabel.isEmpty)
      assert(fa3.deDesc == "test")
      assert(fa3.enDesc.isEmpty)
      assert(rest.isEmpty)
    }

    "parse all in focus_area.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/focus_area.yaml")(
          fileParser.parse
        )
      println(rest)
      assert(res.value.size == 26)
      assert(rest.isEmpty)
    }
  }
}
