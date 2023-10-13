package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parser.ParsingError

final class VersionSchemeParserSpec extends AnyWordSpec with EitherValues {

  "A Version Scheme Parser" should {
    "parse a version scheme if the scheme is valid" in {
      val input = "v1s\n"
      val (res, rest) = VersionSchemeParser.parser.parse(input)
      assert(rest == "\n")
      assert(res.value == VersionScheme(1, "s"))

      val input1 = "v1.5s\n"
      val (res1, rest1) = VersionSchemeParser.parser.parse(input1)
      assert(rest1 == "\n")
      assert(res1.value == VersionScheme(1.5, "s"))
    }

    "fail parsing if the version scheme has no number" in {
      val input = "vs\n"
      val (res, rest) = VersionSchemeParser.parser.parse(input)
      val ParsingError(expected, found) = res.left.value
      assert(rest == input)
      assert(expected == "a double")
      assert(found == "s\n")
    }

    "fail parsing if the version scheme has no label" in {
      val input = "v1\n"
      val (res, rest) = VersionSchemeParser.parser.parse(input)
      val e = res.left.value
      assert(rest == input)
      assert(e.expected == "a given prefix")
      assert(e.found == "\n")
    }
  }
}
