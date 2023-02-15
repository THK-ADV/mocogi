package parsing.metadata

import models.core.Season
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class SeasonParser extends SingleValueParser[Season] {
  def parser(implicit seasons: Seq[Season]): Parser[Season] =
    itemParser(
      "frequency",
      seasons.sortBy(_.abbrev).reverse,
      x => s"season.${x.abbrev}"
    )
}
