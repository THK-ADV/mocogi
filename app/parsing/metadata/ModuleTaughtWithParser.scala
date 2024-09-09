package parsing.metadata

import parser.Parser
import parser.Parser.{prefix, prefixTo, rest}
import parser.ParserOps.P0
import parsing.{multipleValueParser, uuidParser}

import java.util.UUID

object ModuleTaughtWithParser {
  def key = "taught_with"
  def modulePrefix = "module."

  def parser: Parser[List[UUID]] =
    multipleValueParser(
      key,
      prefix(modulePrefix)
        .take(prefixTo("\n").or(rest))
        .flatMap(uuidParser)
    )
}
