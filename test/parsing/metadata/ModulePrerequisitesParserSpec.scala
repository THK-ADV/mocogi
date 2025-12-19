package parsing.metadata

import java.util.UUID

import helper.FakePOs
import models.ModulePrerequisiteEntryProtocol
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.metadata.ModulePrerequisitesParser.recommendedPrerequisitesParser
import parsing.metadata.ModulePrerequisitesParser.recommendedPrerequisitesParserRaw
import parsing.types.ParsedPrerequisiteEntry
import parsing.ParserSpecHelper

class ModulePrerequisitesParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues with FakePOs {

  "A Module Prerequisites Parser" should {
    "parse prerequisites" in {
      val m1    = UUID.randomUUID
      val m2    = UUID.randomUUID
      val input =
        s"""recommended_prerequisites:
           |  text: benötigt werden kenntnisse in algebra und java und ein pc.
           |  modules:
           |    - module.$m1
           |    - module.$m2""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == ParsedPrerequisiteEntry(
          "benötigt werden kenntnisse in algebra und java und ein pc.",
          List(m1, m2)
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites raw" in {
      val m1    = UUID.randomUUID
      val m2    = UUID.randomUUID
      val input =
        s"""recommended_prerequisites:
           |  text: benötigt werden kenntnisse in algebra und java und ein pc.
           |  modules:
           |    - module.$m1
           |    - module.$m2""".stripMargin
      val (res, rest) = recommendedPrerequisitesParserRaw.parse(input)
      assert(
        res.value == ModulePrerequisiteEntryProtocol(
          "benötigt werden kenntnisse in algebra und java und ein pc.",
          List(m1, m2)
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text" in {
      val m1    = UUID.randomUUID
      val m2    = UUID.randomUUID
      val input =
        s"""recommended_prerequisites:
           |  modules:
           |    - module.$m1
           |    - module.$m2""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == ParsedPrerequisiteEntry(
          "",
          List(m1, m2),
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text raw" in {
      val m1    = UUID.randomUUID
      val m2    = UUID.randomUUID
      val input =
        s"""recommended_prerequisites:
           |  modules:
           |    - module.$m1
           |    - module.$m2""".stripMargin
      val (res, rest) = recommendedPrerequisitesParserRaw.parse(input)
      assert(
        res.value == ModulePrerequisiteEntryProtocol(
          "",
          List(m1, m2),
        )
      )
      assert(rest.isEmpty)
    }

    "parse without dashes" in {
      val input = """recommended_prerequisites:
                    |  text: dadasssdasd
                    |  modules: module.5941afae-a356-4dce-9e9b-1b70371c8202
                    |required_prerequisites:
                    |  text: test
                    |  modules: module.438d1da2-cf41-4978-a9d3-e53f74f1e2ad
                    |status: status.inactive""".stripMargin
      val (res, rest) = ModulePrerequisitesParser.parser.parse(input)
      assert(res.isRight)
      assert(rest == "status: status.inactive")
    }
  }
}
