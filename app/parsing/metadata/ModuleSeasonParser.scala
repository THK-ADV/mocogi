package parsing.metadata

import models.core.Season
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class ModuleSeasonParser extends SingleValueParser[Season] {
  def parser(implicit seasons: Seq[Season]): Parser[Season] =
    itemParser(
      "frequency",
      seasons.sortBy(_.id).reverse,
      x => s"season.${x.id}"
    )
}