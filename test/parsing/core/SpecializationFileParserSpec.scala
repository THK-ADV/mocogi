package parsing.core

import helper.FakePOs
import models.core.PO
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.core.SpecializationFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

final class SpecializationFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with OptionValues
    with FakePOs {

  implicit def poAbbrevs(implicit pos: Seq[PO]): Seq[String] =
    pos.map(_.abbrev)

  "A Specialization File Parser" should {
    "parse a single specialization" in {
      val input =
        """inf1_abc:
          |  label: ABC
          |  po: po.inf1""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(rest.isEmpty)
      val entry = res.value.head
      assert(entry.abbrev == "inf1_abc")
      assert(entry.label == "ABC")
      assert(entry.po == "inf1")
    }

    "parse multiple specializations" in {
      val input =
        """inf1_abc:
          |  label: ABC
          |  po: po.inf1
          |
          |wi1_def:
          |  label: WI
          |  po: po.wi1""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(rest.isEmpty)
      val entry1 = res.value.head
      assert(entry1.abbrev == "inf1_abc")
      assert(entry1.label == "ABC")
      assert(entry1.po == "inf1")
      val entry2 = res.value(1)
      assert(entry2.abbrev == "wi1_def")
      assert(entry2.label == "WI")
      assert(entry2.po == "wi1")
    }

    "parse all in specialization.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/specialization.yaml")(fileParser.parse)
      assert(res.value.size == 5)
      assert(rest.isEmpty)
    }
  }
}
