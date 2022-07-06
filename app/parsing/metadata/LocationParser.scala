package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser3
import parsing.types.Location

import javax.inject.Singleton

@Singleton
final class LocationParser extends SimpleFileParser3[Location] {
  def parser(implicit locations: Seq[Location]): Parser[Location] =
    makeTypeParser("location", locations, m => s"location.${m.abbrev}")
}
