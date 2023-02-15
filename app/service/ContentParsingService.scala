package service

import parser.Parser.{skipFirst, zeroOrMoreSpaces}
import parser.ParserOps.P0
import parser.ParsingError
import parsing.content.ContentParser.contentParser
import parsing.types.Content

import javax.inject.Singleton

@Singleton
final class ContentParsingService {
  private val parser =
    skipFirst(zeroOrMoreSpaces)
      .take(contentParser)

  def parse(input: String): (Either[ParsingError, (Content, Content)], Rest) = {
    val (res, rest) = parser.parse(input)
    (res, Rest(rest))
  }
}
