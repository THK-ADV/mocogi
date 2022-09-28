package parsing.metadata

import parser.Parser
import parser.Parser.{prefix, prefixTo, rest}
import parser.ParserOps.P0
import parsing.helper.MultipleValueParser.multipleParser

object TaughtWithParser {
  val taughtWithParser: Parser[List[String]] =
    multipleParser(
      "taught_with",
      prefix("module.")
        .take(prefixTo("\n").or(rest))
    )
}
