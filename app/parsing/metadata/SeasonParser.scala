package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser3
import parsing.types.Season

import javax.inject.Singleton

@Singleton
final class SeasonParser extends SimpleFileParser3[Season] {
  def parser(implicit seasons: Seq[Season]): Parser[Season] =
    makeTypeParser("frequency", seasons, x => s"season.${x.abbrev}")
}
