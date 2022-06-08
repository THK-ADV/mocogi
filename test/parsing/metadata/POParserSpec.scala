package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.POParser.poParser

class POParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues {

  "A PO Parser" should {
    "parse a single po" in {
      val input = "po: AI2\n"
      val (res, rest) = poParser.run(input)
      assert(res.value == List("AI2"))
      assert(rest.isEmpty)
    }

    "parse multiple pos seperated by dashes" in {
      val input =
        """po:
          |-AI2
          |-MI4
          |-WI5
          |""".stripMargin
      val (res, rest) = poParser.run(input)
      assert(res.value == List("AI2", "MI4", "WI5"))
      assert(rest.isEmpty)
    }

    "parse multiple pos seperated by dashes ignoring whitespaces" in {
      val input =
        """po:
          | - AI2
          | - MI4
          | - WI5
          |""".stripMargin
      val (res, rest) = poParser.run(input)
      assert(res.value == List("AI2", "MI4", "WI5"))
      assert(rest.isEmpty)
    }
  }

}
