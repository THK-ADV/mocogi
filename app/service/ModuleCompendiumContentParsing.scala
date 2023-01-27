package service

import parser.Parser.{skipFirst, zeroOrMoreSpaces}
import parser.ParserOps.P0
import parsing.content.ContentParser.contentParser
import parsing.types.Content

import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
final class ModuleCompendiumContentParsing {
  private val parser =
    skipFirst(zeroOrMoreSpaces)
      .take(contentParser)

  def parse(input: String): Future[((Content, Content), String)] = {
    val (res, rest) = parser.parse(input)
    res.fold(
      Future.failed,
      c => Future.successful(c, rest)
    )
  }

  def parse2(input: String)  =
    parser.parse(input)
}
