package parsing.metadata

import models.core.ModuleLocation
import parser.Parser
import parsing.helper.SingleValueParser
import parsing.singleValueRawParser

object ModuleLocationParser extends SingleValueParser[ModuleLocation] {
  def key    = "location"
  def prefix = "location."

  private[parsing] def parser(using locations: Seq[ModuleLocation]): Parser[ModuleLocation] =
    itemParser(
      key,
      locations.sortBy(_.id).reverse,
      m => s"$prefix${m.id}"
    )

  private[parsing] def raw: Parser[String] =
    singleValueRawParser(key, prefix)
}
