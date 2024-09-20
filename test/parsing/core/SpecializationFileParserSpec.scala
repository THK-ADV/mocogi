package parsing.core

import helper.FakePOs
import models.core.PO
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import parsing.core.SpecializationFileParser.fileParser
import parsing.withFile0
import parsing.ParserSpecHelper

final class SpecializationFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with OptionValues
    with FakePOs {

  implicit def poAbbrevs(implicit pos: Seq[PO]): Seq[String] =
    pos.map(_.id)

  "A Specialization File Parser" should {
    "parse a single specialization" in {
      val input =
        """inf1_abc:
          |  label: ABC
          |  po: po.inf1""".stripMargin
      val (res, rest) = fileParser.parse(input)
      assert(rest.isEmpty)
      val entry = res.value.head
      assert(entry.id == "inf1_abc")
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
      assert(entry1.id == "inf1_abc")
      assert(entry1.label == "ABC")
      assert(entry1.po == "inf1")
      val entry2 = res.value(1)
      assert(entry2.id == "wi1_def")
      assert(entry2.label == "WI")
      assert(entry2.po == "wi1")
    }

    "parse all in specialization.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/specialization.yaml")(fileParser.parse)
      assert(rest.isEmpty)
      val first = res.value.find(_.id == "inf_mim4_hci").value
      assert(first.label == "Human-Computer Interaction")
      assert(first.po == "inf1")

      val second = res.value.find(_.id == "inf_mim4_mppd").value
      assert(second.label == "Multiperspective Product Development")
      assert(second.po == "itm1")

      val third = res.value.find(_.id == "inf_mim4_sc").value
      assert(third.label == "Social Computing")
      assert(third.po == "inf1")

      val forth = res.value.find(_.id == "inf_mim4_vc").value
      assert(forth.label == "Visual Computing")
      assert(forth.po == "wi1")

      val fifth = res.value.find(_.id == "inf_mim4_wtw").value
      assert(fifth.label == "Weaving the Web")
      assert(fifth.po == "mi1")
    }
  }
}
