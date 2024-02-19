package parsing.metadata

import models.core.ModuleLocation
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class ModuleLocationParser extends SingleValueParser[ModuleLocation] {
  def parser(implicit locations: Seq[ModuleLocation]): Parser[ModuleLocation] =
    itemParser(
      "location",
      locations.sortBy(_.id).reverse,
      m => s"location.${m.id}"
    )
}
