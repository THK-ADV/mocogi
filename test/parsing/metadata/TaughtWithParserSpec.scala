package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.TaughtWithParser.taughtWithParser

final class TaughtWithParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Taught with Parser" should {
    "parse a single module which is taught with" in {
      val input = "taught_with: module.pp-mi"
      val (res, rest) = taughtWithParser.parse(input)
      assert(rest.isEmpty)
      assert(res.value == List("pp-mi"))
    }

    "parse multiple modules which are taught with" in {
      val input =
        """taught_with:
          |  - module.pp-mi
          |  - module.pp-ai""".stripMargin
      val (res, rest) = taughtWithParser.parse(input)
      assert(rest.isEmpty)
      assert(res.value == List("pp-mi", "pp-ai"))
    }
  }
}
