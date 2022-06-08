package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.PrerequisitesParser.{
  recommendedPrerequisitesParser,
  requiredPrerequisitesParser
}

class PrerequisitesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Prerequisites Parser" when {
    "parse recommended prerequisites" should {
      "return no recommended prerequisites when there is none" in {
        val input = "recommended-prerequisites: none\n"
        val (res, rest) = recommendedPrerequisitesParser.run(input)
        assert(res.value == Nil)
        assert(rest.isEmpty)
      }

      "return a valid recommended prerequisite when there is one" in {
        val input = "recommended-prerequisites: module.ap1\n"
        val (res, rest) = recommendedPrerequisitesParser.run(input)
        assert(res.value == List("ap1"))
        assert(rest.isEmpty)
      }

      "return a valid recommended prerequisite when there is one ignoring whitespaces" in {
        val input = "recommended-prerequisites:  module.ap1\n"
        val (res, rest) = recommendedPrerequisitesParser.run(input)
        assert(res.value == List("ap1"))
        assert(rest.isEmpty)
      }

      "return multiple recommended prerequisites seperated by dashes" in {
        val input =
          """recommended-prerequisites:
            |-module.ap1
            |-module.ap2
            |-module.ma1
            |""".stripMargin
        val (res, rest) = recommendedPrerequisitesParser.run(input)
        assert(res.value == List("ap1", "ap2", "ma1"))
        assert(rest.isEmpty)
      }

      "return multiple recommended prerequisites seperated by dashes ignoring whitespaces" in {
        val input =
          """recommended-prerequisites: 
            | - module.ap1
            | - module.ap2
            | - module.ma1
            |""".stripMargin
        val (res, rest) = recommendedPrerequisitesParser.run(input)
        assert(res.value == List("ap1", "ap2", "ma1"))
        assert(rest.isEmpty)
      }
    }

    "parse required prerequisites" should {
      "return no required prerequisites when there is none" in {
        val input = "required-prerequisites: none\n"
        val (res, rest) = requiredPrerequisitesParser.run(input)
        assert(res.value == Nil)
        assert(rest.isEmpty)
      }

      "return a valid required prerequisite when there is one" in {
        val input = "required-prerequisites: module.ap1\n"
        val (res, rest) = requiredPrerequisitesParser.run(input)
        assert(res.value == List("ap1"))
        assert(rest.isEmpty)
      }

      "return a valid required prerequisite when there is one ignoring whitespaces" in {
        val input = "required-prerequisites:  module.ap1\n"
        val (res, rest) = requiredPrerequisitesParser.run(input)
        assert(res.value == List("ap1"))
        assert(rest.isEmpty)
      }

      "return multiple required prerequisites seperated by dashes" in {
        val input =
          """required-prerequisites:
            |-module.ap1
            |-module.ap2
            |-module.ma1
            |""".stripMargin
        val (res, rest) = requiredPrerequisitesParser.run(input)
        assert(res.value == List("ap1", "ap2", "ma1"))
        assert(rest.isEmpty)
      }

      "return multiple required prerequisites seperated by dashes ignoring whitespaces" in {
        val input =
          """required-prerequisites: 
            | - module.ap1
            | - module.ap2
            | - module.ma1
            |""".stripMargin
        val (res, rest) = requiredPrerequisitesParser.run(input)
        assert(res.value == List("ap1", "ap2", "ma1"))
        assert(rest.isEmpty)
      }
    }
  }
}
