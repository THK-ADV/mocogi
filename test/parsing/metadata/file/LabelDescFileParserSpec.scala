package parsing.metadata.file

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{ParserSpecHelper, withFile0}

final class LabelDescFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  val parser = new LabelDescFileParser[LabelDescImpl] {
    override protected def makeType = LabelDescImpl.tupled
  }.fileParser

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
          LabelDescImpl(
            "entry1",
            "Entry 1",
            "Text1\nText2\n",
            "Entry 1",
            "Text1 Text2\n"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single label-desc with remaining input left" in {
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
          |foo: bar""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          LabelDescImpl(
            "entry1",
            "Entry 1",
            "Text1\nText2\n",
            "Entry 1",
            "Text1 Text2\n"
          )
        )
      )
      assert(rest == "foo: bar")
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
          LabelDescImpl(
            "entry1",
            "Entry 1",
            "Text1\nText2\n",
            "Entry 1",
            "Text1 Text2\n"
          ),
          LabelDescImpl(
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
      assert(
        res.value == List(
          LabelDescImpl(
            "develop-visions",
            "Develop Visions",
            "Desc1\nDesc2\nDesc3\n",
            "Develop Visions",
            "Desc1\nDesc2\n"
          ),
          LabelDescImpl(
            "analyze-domains",
            "Analyze Domains",
            "Desc1\nDesc2\n",
            "Analyze Domains",
            "Desc1\nDesc2\n"
          ),
          LabelDescImpl(
            "digitization",
            "Digitalisierung",
            "Desc1",
            "Digitization",
            ""
          ),
          LabelDescImpl(
            "internationalization",
            "Internationalisierung",
            "Desc1",
            "Internationalization",
            "Desc2"
          ),
        )
      )
      assert(rest.isEmpty)
    }
  }
}
