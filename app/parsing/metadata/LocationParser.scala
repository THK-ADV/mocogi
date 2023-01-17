package parsing.metadata

import basedata.Location
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class LocationParser extends SingleValueParser[Location] {
  def parser(implicit locations: Seq[Location]): Parser[Location] =
    itemParser(
      "location",
      locations.sortBy(_.abbrev).reverse,
      m => s"location.${m.abbrev}"
    )
}
