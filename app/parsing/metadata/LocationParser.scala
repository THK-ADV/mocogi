package parsing.metadata

import models.core.Location
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class LocationParser extends SingleValueParser[Location] {
  def parser(implicit locations: Seq[Location]): Parser[Location] =
    itemParser(
      "location",
      locations.sortBy(_.id).reverse,
      m => s"location.${m.id}"
    )
}
