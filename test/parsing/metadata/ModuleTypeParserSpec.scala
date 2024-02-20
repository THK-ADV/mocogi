package parsing.metadata

import helper.FakeModuleTypes
import models.core.ModuleType
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleTypeParser.raw

class ModuleTypeParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeModuleTypes {

  import ModuleTypeParser.parser

  "A Module Type Parser" should {
    "parse module types if they are valid" in {
      val input1 = "type: type.module\n"
      val (res1, rest1) = parser.parse(input1)
      assert(res1.value == ModuleType("module", "Modul", "--"))
      assert(rest1.isEmpty)

      val input2 = "type: type.generic_module\n"
      val (res2, rest2) = parser.parse(input2)
      assert(
        res2.value == ModuleType("generic_module", "Generisches Modul", "--")
      )
      assert(rest2.isEmpty)
    }

    "parse raw module types" in {
      val input1 = "type: type.module\n"
      val (res1, rest1) = raw.parse(input1)
      assert(res1.value == "module")
      assert(rest1.isEmpty)

      val input2 = "type: type.generic_module\n"
      val (res2, rest2) = raw.parse(input2)
      assert(res2.value == "generic_module")
      assert(rest2.isEmpty)
    }

    "fail if the module type is unknown" in {
      assertError(
        parser,
        "type: type.optional\n",
        "type.module or type.generic_module",
        Some("type.optional\n")
      )
    }
  }
}
