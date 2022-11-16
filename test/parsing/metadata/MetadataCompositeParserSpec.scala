package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parser.ParsingError
import parsing.ParserSpecHelper
import parsing.metadata.mocks.FakeMetadataParser

final class MetadataCompositeParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  val parser = new MetadataCompositeParser(Set(new FakeMetadataParser()))
  val versionSchemeParser = parser.versionSchemeParser
  val metadataParser =
    parser.parser(Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil)

  "A Metadata Composite Parser" should {

    "parse a version scheme if the scheme is valid" in {
      val input = "v1s\n"
      val (res, rest) = versionSchemeParser.parse(input)
      assert(rest == "\n")
      assert(res.value == VersionScheme(1, "s"))

      val input1 = "v1.5s\n"
      val (res1, rest1) = versionSchemeParser.parse(input1)
      assert(rest1 == "\n")
      assert(res1.value == VersionScheme(1.5, "s"))
    }

    "fail parsing if the version scheme has no number" in {
      val input = "vs\n"
      val (res, rest) = versionSchemeParser.parse(input)
      val ParsingError(expected, found) = res.left.value
      assert(rest == input)
      assert(expected == "a double")
      assert(found == "s\n")
    }

    "fail parsing if the version scheme has no label" in {
      val input = "v1\n"
      val (res, rest) = versionSchemeParser.parse(input)
      val e = res.left.value
      assert(rest == input)
      assert(e.expected == "a given prefix")
      assert(e.found == "\n")
    }

    "successfully parse header, metadata and footer" in {
      val input = "---v1s\n---"
      val (res, rest) = metadataParser.parse(input)
      assert(rest.isEmpty)
      assert(res.isRight)
    }

    "fail if the version scheme is not found" in {
      val input = "---v1x\n---"
      val (res, rest) = metadataParser.parse(input)
      val e = res.left.value
      assert(rest == input)
      assert(e.expected == "unknown version scheme v1.0x")
      assert(e.found == "---")
    }

    "fail if version scheme is missing" in {
      val input = "---\n---"
      val (res, rest) = metadataParser.parse(input)
      val e = res.left.value
      assert(rest == input)
      assert(e.expected == "v")
      assert(e.found == "\n---")
    }
  }
}
