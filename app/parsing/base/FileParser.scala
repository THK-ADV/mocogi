package parsing.base

import parser.Parser

trait FileParser[A] {
  val fileParser: Parser[List[A]]
}
