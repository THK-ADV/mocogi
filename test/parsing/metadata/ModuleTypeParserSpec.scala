package parsing.metadata

import helper.{FakeApplication, FakeModuleTypes}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper
import parsing.types.ModuleType

class ModuleTypeParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeModuleTypes {

  val parser = app.injector.instanceOf(classOf[ModuleTypeParser]).parser

  "A Module Type Parser" should {
    "parse module types if they are valid" in {
      val (res1, rest1) =
        parser.parse("type: type.module\n")
      assert(res1.value == ModuleType("module", "Modul", "--"))
      assert(rest1.isEmpty)

      val (res2, rest2) =
        parser.parse("type: type.generic_module\n")
      assert(res2.value == ModuleType("generic_module", "Generisches Modul", "--"))
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
