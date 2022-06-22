package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Season

import javax.inject.Singleton

trait SeasonParser {
  val fileParser: Parser[List[Season]]
  val parser: Parser[Season]
}

@Singleton
final class SeasonParserImpl(val path: String)
    extends SeasonParser
    with SimpleFileParser[Season] {

  override val makeType = Season.tupled
  override val typename = "seasons"

  lazy val seasons: List[Season] = parseTypes

  val fileParser: Parser[List[Season]] = makeFileParser

  val parser: Parser[Season] =
    makeTypeParser("frequency")(t => s"season.${t.abbrev}")
}
