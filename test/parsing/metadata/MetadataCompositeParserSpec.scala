package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.mocks.FakeMetadataParser

final class MetadataCompositeParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  val parser = new MetadataCompositeParser(Set(new FakeMetadataParser()))
  val metadataParser =
    parser.parser(Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil, Nil)

  "A Metadata Composite Parser" should {

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
