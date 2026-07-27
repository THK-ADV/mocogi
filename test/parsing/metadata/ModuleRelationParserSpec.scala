package parsing.metadata

import java.util.UUID

import cats.data.NonEmptyList
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import parsing.metadata.ModuleRelationParser.parser
import parsing.ParserSpecHelper

class ModuleRelationParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues with OptionValues {

  "A Module Relation Parser" should {
    "parse a super module with its children" in {
      val m1     = UUID.randomUUID
      val m2     = UUID.randomUUID
      val input1 =
        s"""relation:
           | children:
           |  - module.$m1
           |  - module.$m2\n""".stripMargin
      val (res1, rest1) = parser.parse(input1)
      assert(
        res1.value.value == NonEmptyList.of(m1, m2)
      )
      assert(rest1.isEmpty)

      val input2 =
        s"""relation:
           | children:
           |  - module.$m1\n""".stripMargin
      val (res2, rest2) = parser.parse(input2)
      assert(
        res2.value.value == NonEmptyList.one(m1)
      )
      assert(rest2.isEmpty)

      val input3 =
        s"""relation:
           | children: module.$m1\n""".stripMargin
      val (res3, rest3) = parser.parse(input3)
      assert(
        res3.value.value == NonEmptyList.one(m1)
      )
      assert(rest3.isEmpty)
    }

    "ignore and consume a legacy child relation" in {
      val m1    = UUID.randomUUID
      val input =
        s"""relation:
           | parent: module.$m1""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(res.value.isEmpty)
      assert(rest.isEmpty)
    }

    "return none if there is no module relation" in {
      val input =
        """module_stuff: abc""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(res.value.isEmpty)
      assert(rest == input)
    }
  }
}
