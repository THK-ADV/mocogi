package parsing.metadata

import models.core.Season
import parser.Parser
import parsing.helper.SingleValueParser
import parsing.singleValueRawParser

object ModuleSeasonParser extends SingleValueParser[Season] {
  def key = "frequency"
  def prefix = "season."

  def parser(implicit seasons: Seq[Season]): Parser[Season] = {
    itemParser(
      key,
      seasons.sortBy(_.id).reverse,
      x => s"$prefix${x.id}"
    )
  }

  def raw: Parser[String] =
    singleValueRawParser(key, prefix)
}
