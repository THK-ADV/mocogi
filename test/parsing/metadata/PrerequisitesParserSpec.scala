package parsing.metadata

import helper.FakePOs
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.PrerequisitesParser.recommendedPrerequisitesParser
import parsing.types.ParsedPrerequisiteEntry

import java.util.UUID

class PrerequisitesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakePOs {

  "A Prerequisites Parser" should {
    "parse prerequisites" in {
      val m1 = UUID.randomUUID
      val m2 = UUID.randomUUID
      val input =
        s"""recommended_prerequisites:
          |  text: >
          |    benötigt werden kenntnisse in algebra und java
          |
          |    und ein pc.
          |  modules:
          |    - module.$m1
          |    - module.$m2
          |  study_programs:
          |    - study_program.mi1""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == ParsedPrerequisiteEntry(
          "benötigt werden kenntnisse in algebra und java\nund ein pc.\n",
          List(m1, m2),
          List(mi1)
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text and no study programs" in {
      val m1 = UUID.randomUUID
      val m2 = UUID.randomUUID
      val input =
        s"""recommended_prerequisites:
          |  modules:
          |    - module.$m1
          |    - module.$m2
          |  """.stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == ParsedPrerequisiteEntry(
          "",
          List(m1, m2),
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
