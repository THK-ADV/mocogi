package parsing.metadata.file

import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.types.ModuleType
import parsing.{ParserSpecHelper, withFile0}

class ModuleTypeFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[ModuleTypeFileParser]).fileParser

  "A Module Type Parser" should {
    "parse a single module type" in {
      val input =
        """mandatory:
          |  de_label: Pflicht
          |  en_label: mandatory""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(ModuleType("mandatory", "Pflicht", "mandatory"))
      )
      assert(rest.isEmpty)
    }

    "parse multiple module types" in {
      val input =
        """mandatory:
          |  de_label: Pflicht
          |  en_label: mandatory
          |wpf:
          |  de_label: Wahlpflichtfach
          |  en_label: choosable course""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          ModuleType("mandatory", "Pflicht", "mandatory"),
          ModuleType("wpf", "Wahlpflichtfach", "choosable course")
        )
      )
      assert(rest.isEmpty)
    }

    "return all modules in module_type.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/module_type.yaml")(parser.parse)
      assert(
        res.value == List(
          ModuleType("mandatory", "Pflicht", "--"),
          ModuleType("wpf", "Wahlpflichtfach", "--")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
