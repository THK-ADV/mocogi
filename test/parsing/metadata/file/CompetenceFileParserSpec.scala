package parsing.metadata.file

import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.types.Competence
import parsing.{ParserSpecHelper, withFile0}

class CompetenceFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[CompetenceFileParser]).fileParser

  "A Competence File Parser" should {
    "parse a single competence" in {
      val input =
        """comp1:
          |  de_label: Kompetenz 1
          |  de_desc: |
          |    Text1
          |    Text2
          |  en_label: Competence 1
          |  en_desc: >
          |    Text1
          |    Text2""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          Competence(
            "comp1",
            "Kompetenz 1",
            "Text1\nText2\n",
            "Competence 1",
            "Text1 Text2\n"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single competence with remaining input left" in {
      val input =
        """comp1:
          |  de_label: Kompetenz 1
          |  de_desc: |
          |    Text1
          |    Text2
          |  en_label: Competence 1
          |  en_desc: >
          |    Text1
          |    Text2
          |foo: bar""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          Competence(
            "comp1",
            "Kompetenz 1",
            "Text1\nText2\n",
            "Competence 1",
            "Text1 Text2\n"
          )
        )
      )
      assert(rest == "foo: bar")
    }

    "parse a multiple competences in" in {
      val input =
        """comp1:
          |  de_label: Kompetenz 1
          |  de_desc: |
          |    Text1
          |    Text2
          |  en_label: Competence 1
          |  en_desc: >
          |    Text1
          |    Text2
          |comp2:
          |  de_label: Kompetenz 2
          |  de_desc:
          |    Text1
          |    Text2
          |  en_label: Competence 2
          |  en_desc:
          |    Text1
          |    Text2""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          Competence(
            "comp1",
            "Kompetenz 1",
            "Text1\nText2\n",
            "Competence 1",
            "Text1 Text2\n"
          ),
          Competence(
            "comp2",
            "Kompetenz 2",
            "Text1 Text2",
            "Competence 2",
            "Text1 Text2"
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse all competences in competences.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/competences.yaml")(parser.parse)
      assert(
        res.value == List(
          Competence(
            "develop-visions",
            "Develop Visions",
            "Desc1\nDesc2\nDesc3\n",
            "Develop Visions",
            "Desc1\nDesc2\n"
          ),
          Competence(
            "analyze-domains",
            "Analyze Domains",
            "Desc1\nDesc2\n",
            "Analyze Domains",
            "Desc1\nDesc2\n"
          )
        )
      )
      assert(rest.isEmpty)
    }
  }
}
