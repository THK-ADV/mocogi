package parsing.metadata

import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.types.ModuleType
import parsing.{ParserSpecHelper, withFile0}

class ModuleTypeParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[ModuleTypeParser])

  val moduleTypesFileParser = parser.fileParser
  val moduleTypeParser = parser.parser

  "A Module Type Parser" should {
    "parse modules type file" when {
      "parse a single module type" in {
        val input =
          """mandatory:
            |  de_label: Pflicht""".stripMargin
        val (res, rest) = moduleTypesFileParser.parse(input)
        assert(
          res.value == List(ModuleType("mandatory", "Pflicht"))
        )
        assert(rest.isEmpty)
      }

      "parse multiple module types" in {
        val input =
          """mandatory:
            |  de_label: Pflicht
            |wpf:
            |  de_label: Wahlpflichtfach""".stripMargin
        val (res, rest) = moduleTypesFileParser.parse(input)
        assert(
          res.value == List(
            ModuleType("mandatory", "Pflicht"),
            ModuleType("wpf", "Wahlpflichtfach")
          )
        )
        assert(rest.isEmpty)
      }

      "parse all module types" in {
        val input =
          """mandatory:
            |  de_label: Pflicht
            |
            |wpf:
            |  de_label: Wahlpflichtfach
            |
            |submodule:
            |  de_label: Untermodul
            |
            |supermodule:
            |  de_label: Obermodul""".stripMargin
        val (res, rest) = moduleTypesFileParser.parse(input)
        assert(
          res.value == List(
            ModuleType("mandatory", "Pflicht"),
            ModuleType("wpf", "Wahlpflichtfach"),
            ModuleType("submodule", "Untermodul"),
            ModuleType("supermodule", "Obermodul")
          )
        )
        assert(rest.isEmpty)
      }

      "return all modules in module_type.yaml" in {
        val (res, rest) =
          withFile0("test/parsing/res/module_type.yaml")(moduleTypesFileParser.parse)
        assert(
          res.value == List(
            ModuleType("mandatory", "Pflicht"),
            ModuleType("wpf", "Wahlpflichtfach")
          )
        )
        assert(rest.isEmpty)
      }
    }

    "parse module type" should {
      "return module types if they are valid" in {
        val (res1, rest1) =
          moduleTypeParser.parse("module_type: module_type.mandatory\n")
        assert(res1.value == ModuleType("mandatory", "Pflicht"))
        assert(rest1.isEmpty)

        val (res2, rest2) =
          moduleTypeParser.parse("module_type: module_type.wpf\n")
        assert(res2.value == ModuleType("wpf", "Wahlpflichtfach"))
        assert(rest2.isEmpty)
      }

      "fail if the module type is unknown" in {
        assertError(
          moduleTypeParser,
          "module_type: module_type.optional\n",
          "module_type.mandatory or module_type.wpf"
        )
      }
    }
  }
}
