package parsing.metadata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.metadata.ModuleECTSParser.*
import parsing.ParserSpecHelper

final class ModuleECTSParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues {

  "A ECTS Parser" should {
    "parse a simple ects value" in {
      val (res1, rest1) = parser.parse("ects: 5")
      assert(res1.value == 5)
      assert(rest1.isEmpty)
      val (res2, rest2) = parser.parse("ects: -5")
      assert(res2.value == -5)
      assert(rest2.isEmpty)
      val (res3, rest3) = parser.parse("ects: 2.5")
      assert(res3.value == 2.5)
      assert(rest3.isEmpty)
    }
  }
}
