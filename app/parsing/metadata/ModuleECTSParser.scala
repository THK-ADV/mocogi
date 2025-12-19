package parsing.metadata

import parser.Parser
import parsing.doubleForKey

object ModuleECTSParser {

  def key = "ects"

  private[parsing] def raw: Parser[Double] =
    parser

  private[parsing] def parser: Parser[Double] =
    doubleForKey(key)
}
