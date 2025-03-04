package parsing.metadata

import parser.Parser
import parsing.doubleForKey

object ModuleECTSParser {

  def key = "ects"

  def raw: Parser[Double] =
    parser

  def parser: Parser[Double] = {
    doubleForKey(key)
  }
}
