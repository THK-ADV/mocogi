package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Location

import javax.inject.Singleton

trait LocationParser {
  val fileParser: Parser[List[Location]]
  val parser: Parser[Location]
}

@Singleton
final class LocationParserImpl(val path: String)
    extends LocationParser
    with SimpleFileParser[Location] {
  override protected val makeType = Location.tupled

  override protected val typename = "locations"

  val fileParser: Parser[List[Location]] = makeFileParser

  val locations: List[Location] = parseTypes

  val parser: Parser[Location] =
    makeTypeParser("location")(t => s"location.${t.abbrev}")
}
