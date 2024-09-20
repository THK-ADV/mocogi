package parsing.core

import helper.FakeStudyPrograms
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import parsing.core.FocusAreaFileParser.fileParser
import parsing.withFile0
import parsing.ParserSpecHelper

final class FocusAreaFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeStudyPrograms
    with OptionValues {
  "A Focus Area File Parser" should {
    "parse a single focus area with all fields" in {
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
      val fa          = res.value.head
      assert(fa.id == "ar")
      assert(fa.program == itm)
      assert(fa.deLabel == "ok")
      assert(fa.enLabel == "ok")
      assert(fa.deDesc == "abc\ndef\n")
      assert(fa.enDesc == "ok\nko")
      assert(rest.isEmpty)
    }

    "parse a single focus area with only required fields" in {
      val input =
        """# comment
          |ar:
          |  program: program.inf_itm
          |  de_label: ok""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val fa          = res.value.head
      assert(fa.id == "ar")
      assert(fa.program == itm)
      assert(fa.deLabel == "ok")
      assert(fa.enLabel.isEmpty)
      assert(fa.deDesc.isEmpty)
      assert(fa.enDesc.isEmpty)
      assert(rest.isEmpty)
    }

    "fail parsing if study program can't be found" in {
      val input =
        """# comment
          |ar:
          |  program: program.inf_abc
          |  de_label: ok
          |  en_label: ok
          |  de_desc: ok
          |  en_desc: ok""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(
        res.left.value.expected == "inf_inf, inf_itm, inf_mi, inf_wi, inf_mim"
      )
      assert(res.left.value.found == "program.inf_abc")
      assert(rest == input)
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
      val fa1         = res.value.head
      assert(fa1.id == "ar")
      assert(fa1.program == itm)
      assert(fa1.deLabel == "ok")
      assert(fa1.enLabel == "ok")
      assert(fa1.deDesc.isEmpty)
      assert(fa1.enDesc.isEmpty)

      val fa2 = res.value(1)
      assert(fa2.id == "pup")
      assert(fa2.program == itm)
      assert(fa2.deLabel == "ok")
      assert(fa2.enLabel.isEmpty)
      assert(fa2.deDesc.isEmpty)
      assert(fa2.enDesc.isEmpty)

      val fa3 = res.value(2)
      assert(fa3.id == "bui")
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
      val fas = res.value
      assert(fas.size == 31)
      assert(rest.isEmpty)

      val ids = List(
        "ar",
        "acs",
        "dip",
        "eb",
        "gak",
        "mri",
        "ppr",
        "bin",
        "ipp",
        "man",
        "con",
        "een",
        "ele",
        "mas",
        "een-wiw",
        "uin",
        "iwe",
        "dmo",
        "ere",
        "sc",
        "vc",
        "ww",
        "dux",
        "dev",
        "crea",
        "indi",
        "exa",
        "fen",
        "pup",
        "bui",
        "ppb"
      )

      fas.zip(ids).zipWithIndex.foreach {
        case ((fa, id), idx) =>
          assert(fa.id == id, idx)
          assert(fa.program == "inf_inf")
      }

      val ar = fas.find(_.id == "ar").value
      assert(ar.deLabel == "Acting Responsibly")
      assert(ar.enLabel == "Acting Responsibly")
      assert(ar.deDesc.startsWith("Professionelles Handeln im Bereich"))
      assert(ar.enDesc.startsWith("Professional action in the field of"))

      val ppr = fas.find(_.id == "ppr").value
      assert(ppr.deLabel == "Planung und Projekierung")
      assert(ppr.enLabel.isEmpty)
      assert(ppr.deDesc.startsWith("In der Elektrotechnik und in der"))
      assert(ppr.enDesc.isEmpty)

      val man = fas.find(_.id == "man").value
      assert(man.deLabel == "Fertigung")
      assert(man.enLabel == "Manufacturing")
      assert(man.deDesc.startsWith("Im Handlungsfeld Fertigung sollen"))
      assert(man.enDesc.isEmpty)

      val ele = fas.find(_.id == "ele").value
      assert(ele.deLabel == "Elektrotechnik")
      assert(ele.enLabel.isEmpty)
      assert(ele.deDesc.isEmpty)
      assert(ele.enDesc.isEmpty)

      val een_wiw = fas.find(_.id == "een-wiw").value
      assert(een_wiw.deLabel == "Umwelttechnik")
      assert(een_wiw.enLabel == "Environmental Engineering")
      assert(een_wiw.deDesc.isEmpty)
      assert(een_wiw.enDesc.isEmpty)

      val exa = fas.find(_.id == "exa").value
      assert(exa.deLabel == "Exploring advanced interactive Media")
      assert(exa.enLabel.isEmpty)
      assert(exa.deDesc.startsWith("Im Handlungsfeld Exploring"))
      assert(exa.deDesc.endsWith("Stand der Technik hinaus.\n"))
      assert(exa.enDesc.isEmpty)

      val fen = fas.find(_.id == "fen").value
      assert(fen.deLabel == "Forschung und Entwicklung")
      assert(fen.enLabel.isEmpty)
      assert(fen.deDesc.startsWith("\"Forschung und Entwicklung (F&E)"))
      assert(fen.deDesc.endsWith("Gelegenheit zur Promotion gaben.\n"))
      assert(fen.enDesc.isEmpty)
    }
  }
}
