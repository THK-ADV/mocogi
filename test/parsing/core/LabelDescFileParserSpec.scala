package parsing.core

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parser.Parser
import parsing.withFile0
import parsing.ParserSpecHelper

final class LabelDescFileParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues {

  private class Impl extends LabelDescFileParser[IDLabelDescImpl] {
    def parser(): Parser[List[IDLabelDescImpl]] = super.fileParser()

    protected override def makeType = {
      case (id, deLabel, enLabel, deDesc, enDesc) =>
        IDLabelDescImpl(id, deLabel, deDesc, enLabel, enDesc)
    }
  }

  val parser: Parser[List[IDLabelDescImpl]] = new Impl().parser()

  "A Label Desc File Parser" should {
    "parse a single label-desc" in {
      val input =
        """entry1:
          |  de_label: Entry 1
          |  de_desc: |
          |    Text1
          |    Text2
          |  en_label: Entry 1
          |  en_desc: >
          |    Text1
          |    Text2""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          IDLabelDescImpl(
            "entry1",
            "Entry 1",
            "Text1\nText2",
            "Entry 1",
            "Text1 Text2"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a multiple label-descs in" in {
      val input =
        """entry1:
          |  de_label: Entry 1
          |  de_desc: |
          |    Text1
          |    Text2
          |  en_label: Entry 1
          |  en_desc: >
          |    Text1
          |    Text2
          |entry2:
          |  de_label: Entry 2
          |  de_desc:
          |    Text1
          |    Text2
          |  en_label: Entry 2
          |  en_desc:
          |    Text1
          |    Text2""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          IDLabelDescImpl(
            "entry1",
            "Entry 1",
            "Text1\nText2",
            "Entry 1",
            "Text1 Text2"
          ),
          IDLabelDescImpl(
            "entry2",
            "Entry 2",
            "Text1 Text2",
            "Entry 2",
            "Text1 Text2"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse all label-descs in label_desc.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/label_desc.yaml")(parser.parse)
      val first = res.value.head
      assert(first.id == "develop-visions")
      assert(first.deLabel == "Develop Visions")
      assert(first.deDesc == "Desc1\nDesc2\nDesc3")
      assert(first.enLabel == "Develop Visions")
      assert(first.enDesc == "Desc1\nDesc2")

      val second = res.value(1)
      assert(second.id == "analyze-domains")
      assert(second.deLabel == "Analyze Domains")
      assert(second.deDesc == "Desc1\nDesc2")
      assert(second.enLabel == "Analyze Domains")
      assert(second.enDesc == "Desc1\nDesc2")

      val third = res.value(2)
      assert(third.id == "digitization")
      assert(third.deLabel == "Digitalisierung")
      assert(third.deDesc == "Desc1")
      assert(third.enLabel == "Digitization")
      assert(third.enDesc == "")

      val forth = res.value(3)
      assert(forth.id == "internationalization")
      assert(forth.deLabel == "Internationalisierung")
      assert(forth.deDesc == "Desc1")
      assert(forth.enLabel == "Internationalization")
      assert(forth.enDesc == "Desc2")

      assert(rest.isEmpty)
    }
  }
}
