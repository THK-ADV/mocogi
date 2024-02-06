package service

import parser.Parser.{skipFirst, zeroOrMoreSpaces}
import parser.ParserOps.P0
import parser.ParsingError
import parsing.content.ModuleContentParser.contentParser
import parsing.types.ModuleContent

import javax.inject.Singleton

@Singleton
final class ContentParsingService {
  private val parser =
    skipFirst(zeroOrMoreSpaces)
      .take(contentParser)

  def parse(input: String): (Either[ParsingError, (ModuleContent, ModuleContent)], Rest) = {
    val (res, rest) = parser.parse(input)
    (res, Rest(rest))
  }
}
