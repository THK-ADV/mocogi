package parsing.metadata.file

import parser.Parser

trait FileParser[A] {
  val fileParser: Parser[List[A]]
}
