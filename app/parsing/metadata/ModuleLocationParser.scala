package parsing.metadata

import models.core.ModuleLocation
import parser.Parser
import parsing.helper.SingleValueParser
import parsing.singleValueRawParser

object ModuleLocationParser extends SingleValueParser[ModuleLocation] {
  private def key = "location"
  private def prefix = "location."

  def parser(implicit locations: Seq[ModuleLocation]): Parser[ModuleLocation] =
    itemParser(
      key,
      locations.sortBy(_.id).reverse,
      m => s"$prefix${m.id}"
    )

  def raw: Parser[String] =
    singleValueRawParser(key, prefix)
}
