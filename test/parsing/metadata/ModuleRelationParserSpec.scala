package parsing.metadata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.ParserSpecHelper
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.types.ModuleRelation

class ModuleRelationParserSpec extends AnyWordSpec
  with ParserSpecHelper
  with EitherValues
  with OptionValues {

  "A Module Relation Parser" should {
    "parse a super module with its children" in {
      val input1 =
        """relation:
          | children:
          |  - module.abc
          |  - module.def""".stripMargin
      val (res1, rest1) = moduleRelationParser.parse(input1)
      assert(res1.value.value == ModuleRelation.Parent(List("abc", "def")))
      assert(rest1.isEmpty)

      val input2 =
        """relation:
          | children:
          |  - module.abc""".stripMargin
      val (res2, rest2) = moduleRelationParser.parse(input2)
      assert(res2.value.value == ModuleRelation.Parent(List("abc")))
      assert(rest2.isEmpty)
    }

    "parse a sub module with its parent" in {
      val input =
        """relation:
          | parent: module.abc""".stripMargin
      val (res, rest) = moduleRelationParser.parse(input)
      assert(res.value.value == ModuleRelation.Child("abc"))
      assert(rest.isEmpty)
    }

    "return none if the module is neither a parent nor a child" in {
      val input =
        """module_stuff: abc""".stripMargin
      val (res, rest) = moduleRelationParser.parse(input)
      assert(res.value.isEmpty)
      assert(rest == input)
    }
  }
}
