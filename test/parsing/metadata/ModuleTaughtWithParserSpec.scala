package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleTaughtWithParser.taughtWithParser

import java.util.UUID

final class ModuleTaughtWithParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Taught with Parser" should {
    "parse a single module which is taught with" in {
      val m1 = UUID.randomUUID
      val input = s"taught_with: module.$m1"
      val (res, rest) = taughtWithParser.parse(input)
      assert(rest.isEmpty)
      assert(res.value == List(m1))
    }

    "parse multiple modules which are taught with" in {
      val m1 = UUID.randomUUID
      val m2 = UUID.randomUUID
      val input =
        s"""taught_with:
          |  - module.$m1
          |  - module.$m2""".stripMargin
      val (res, rest) = taughtWithParser.parse(input)
      assert(rest.isEmpty)
      assert(res.value == List(m1, m2))
    }
  }
}
