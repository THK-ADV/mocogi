package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileItemParser
import parsing.types.Season

import javax.inject.Singleton

@Singleton
final class SeasonParser extends SimpleFileItemParser[Season] {
  def parser(implicit seasons: Seq[Season]): Parser[Season] =
    itemParser("frequency", seasons, x => s"season.${x.abbrev}")
}
