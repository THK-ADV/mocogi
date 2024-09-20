package parsing.metadata

import parser.Parser
import parser.Parser.double
import parser.Parser.prefix
import parser.ParserOps.P0

object VersionSchemeParser {
  def parser: Parser[VersionScheme] =
    prefix("v")
      .take(double)
      .zip(prefix(_ != '\n'))
      .map(VersionScheme.apply)
}
