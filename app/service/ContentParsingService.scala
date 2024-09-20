package service

import parser.Parser
import parser.Parser.skipFirst
import parser.Parser.zeroOrMoreSpaces
import parser.ParserOps.P0
import parser.ParsingError
import parsing.content.ModuleContentParser.contentParser
import parsing.types.ModuleContent

object ContentParsingService {
  def parser: Parser[(ModuleContent, ModuleContent)] =
    skipFirst(zeroOrMoreSpaces)
      .take(contentParser)

  def parse(
      input: String
  ): (Either[ParsingError, (ModuleContent, ModuleContent)], Rest) = {
    val (res, rest) = parser.parse(input)
    (res, Rest(rest))
  }
}
