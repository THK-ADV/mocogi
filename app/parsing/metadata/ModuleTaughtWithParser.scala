package parsing.metadata

import java.util.UUID

import parser.Parser
import parser.Parser.prefix
import parser.Parser.prefixTo
import parser.Parser.rest
import parser.ParserOps.P0
import parsing.multipleValueParser
import parsing.uuidParser

object ModuleTaughtWithParser {
  def key          = "taught_with"
  def modulePrefix = "module."

  def parser: Parser[List[UUID]] =
    multipleValueParser(
      key,
      prefix(modulePrefix)
        .take(prefixTo("\n").or(rest))
        .flatMap(uuidParser)
    )
}
