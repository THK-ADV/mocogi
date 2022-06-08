package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Location

object LocationParser extends SimpleFileParser[Location] {
  override protected val makeType = Location.tupled

  override protected val filename = "location.yaml"

  override protected val typename = "locations"

  val locationFileParser: Parser[List[Location]] = fileParser

  val locations: List[Location] = types

  val locationParser: Parser[Location] =
    typeParser("location")(t => s"location.${t.abbrev}")
}
