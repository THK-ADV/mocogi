package parsing.metadata

import basedata.Season
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class SeasonParser extends SingleValueParser[Season] {
  def parser(implicit seasons: Seq[Season]): Parser[Season] =
    itemParser("frequency", seasons, x => s"season.${x.abbrev}")
}
