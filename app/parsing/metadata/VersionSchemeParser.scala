package parsing.metadata

import parser.Parser
import parser.Parser.{double, prefix}
import parser.ParserOps.P0

object VersionSchemeParser {
  val parser: Parser[VersionScheme] =
    prefix("v")
      .take(double)
      .zip(prefix(_ != '\n'))
      .map(VersionScheme.tupled)
}
