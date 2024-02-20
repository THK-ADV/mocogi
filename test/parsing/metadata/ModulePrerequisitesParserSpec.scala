package parsing.metadata

import helper.FakePOs
import models.ModulePrerequisiteEntryProtocol
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModulePrerequisitesParser.{
  recommendedPrerequisitesParser,
  recommendedPrerequisitesParserRaw
}
import parsing.types.ParsedPrerequisiteEntry

import java.util.UUID

class ModulePrerequisitesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakePOs {

  "A Module Prerequisites Parser" should {
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

    "parse prerequisites raw" in {
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
      val (res, rest) = recommendedPrerequisitesParserRaw.parse(input)
      assert(
        res.value == ModulePrerequisiteEntryProtocol(
          "benötigt werden kenntnisse in algebra und java\nund ein pc.\n",
          List(m1, m2),
          List("mi1")
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

    "parse prerequisites with not text and no study programs raw" in {
      val m1 = UUID.randomUUID
      val m2 = UUID.randomUUID
      val input =
        s"""recommended_prerequisites:
          |  modules:
          |    - module.$m1
          |    - module.$m2
          |  """.stripMargin
      val (res, rest) = recommendedPrerequisitesParserRaw.parse(input)
      assert(
        res.value == ModulePrerequisiteEntryProtocol(
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

    "parse prerequisites with not text and no modules raw" in {
      val input =
        """recommended_prerequisites:
          |  study_programs:
          |    - study_program.mi1""".stripMargin
      val (res, rest) = recommendedPrerequisitesParserRaw.parse(input)
      assert(
        res.value == ModulePrerequisiteEntryProtocol(
          "",
          Nil,
          List("mi1")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
