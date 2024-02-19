package parsing.metadata

import parser.Parser
import parser.Parser.{prefix, prefixTo, rest}
import parser.ParserOps.P0
import parsing.{multipleValueParser, uuidParser}

import java.util.UUID

object ModuleTaughtWithParser {
  def taughtWithParser: Parser[List[UUID]] =
    multipleValueParser(
      "taught_with",
      prefix("module.")
        .take(prefixTo("\n").or(rest))
        .flatMap(uuidParser)
    )
}
