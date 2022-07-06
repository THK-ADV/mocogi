package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileItemParser
import parsing.types.Location

import javax.inject.Singleton

@Singleton
final class LocationParser extends SimpleFileItemParser[Location] {
  def parser(implicit locations: Seq[Location]): Parser[Location] =
    itemParser("location", locations, m => s"location.${m.abbrev}")
}
