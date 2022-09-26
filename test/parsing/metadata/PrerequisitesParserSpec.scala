package parsing.metadata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.ParserSpecHelper
import parsing.metadata.PrerequisitesParser.recommendedPrerequisitesParser
import parsing.types.Prerequisites

class PrerequisitesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with OptionValues {

  "A Prerequisites Parser" should {
    "parse no prerequisites when there is none" in {
      val input = "recommended_prerequisites: none\n"
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(res.value.isEmpty)
      assert(rest.isEmpty)
    }

    "parse prerequisites" in {
      val input =
        """recommended_prerequisites:
          |  text: >
          |    benötigt werden kenntnisse in algebra und java
          |
          |    und ein pc.
          |  modules:
          |    - module.ap1
          |    - module.ap2
          |  study_programs:
          |    - study_program.mi
          |  """.stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value.value == Prerequisites(
          "benötigt werden kenntnisse in algebra und java\nund ein pc.\n",
          List("ap1", "ap2"),
          List("mi")
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text and no study programs" in {
      val input =
        """recommended_prerequisites:
          |  text:
          |  modules:
          |    - module.ap1
          |    - module.ap2
          |  study_programs:""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value.value == Prerequisites(
          "",
          List("ap1", "ap2"),
          Nil
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text and no modules" in {
      val input =
        """recommended_prerequisites:
          |  text:
          |  modules:
          |  study_programs:
          |    - study_program.mi
          |  """.stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value.value == Prerequisites(
          "",
          Nil,
          List("mi")
        )
      )
      assert(rest.isEmpty)
    }

    "parse empty prerequisites" in {
      val input =
        """recommended_prerequisites:
          |  text:
          |  modules:
          |  study_programs:""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(res.value.value == Prerequisites("", Nil, Nil))
      assert(rest.isEmpty)
    }
  }
}
