package parsing.core

import models.core.ModuleType
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{ParserSpecHelper, withFile0}

class ModuleTypeFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  val parser = ModuleTypeFileParser.parser()

  "A Module Type Parser" should {
    "parse a single module type" in {
      val input =
        """module:
          |  de_label: Modul
          |  en_label: Module""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(ModuleType("module", "Modul", "Module"))
      )
      assert(rest.isEmpty)
    }

    "parse multiple module types" in {
      val input =
        """module:
          |  de_label: Modul
          |  en_label: Module
          |generic_module:
          |  de_label: Generisches Modul
          |  en_label: generic module""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          ModuleType("module", "Modul", "Module"),
          ModuleType("generic_module", "Generisches Modul", "generic module")
        )
      )
      assert(rest.isEmpty)
    }

    "return all modules in types.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/types.yaml")(parser.parse)
      assert(
        res.value == List(
          ModuleType("module", "Module", ""),
          ModuleType("generic_module", "Generisches Modul", "")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
