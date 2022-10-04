package parsing.metadata

import helper.FakePOs
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.PrerequisitesParser.recommendedPrerequisitesParser
import parsing.types.ParsedPrerequisiteEntry

class PrerequisitesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakePOs {

  "A Prerequisites Parser" should {
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
          |    - study_program.mi1""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == ParsedPrerequisiteEntry(
          "benötigt werden kenntnisse in algebra und java\nund ein pc.\n",
          List("ap1", "ap2"),
          List(mi1)
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text and no study programs" in {
      val input =
        """recommended_prerequisites:
          |  modules:
          |    - module.ap1
          |    - module.ap2
          |  """.stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == ParsedPrerequisiteEntry(
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
          |  study_programs:
          |    - study_program.mi1""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == ParsedPrerequisiteEntry(
          "",
          Nil,
          List(mi1)
        )
      )
      assert(rest.isEmpty)
    }
  }
}
