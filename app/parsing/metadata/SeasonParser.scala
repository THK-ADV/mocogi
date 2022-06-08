package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Season

object SeasonParser extends SimpleFileParser[Season] {

  override val makeType = Season.tupled
  override val filename = "season.yaml"
  override val typename = "seasons"

  val seasonFileParser: Parser[List[Season]] = fileParser

  val seasons: List[Season] = types

  val seasonParser: Parser[Season] =
    typeParser("frequency")(t => s"season.${t.abbrev}")
}
