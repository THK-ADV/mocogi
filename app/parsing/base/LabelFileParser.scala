package parsing.base

import parser.Parser
import parser.Parser.{newline, optional, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.P2
import parsing.singleLineStringForKey

trait LabelFileParser[A] extends FileParser[A] {
  protected def makeType: ((String, String, String)) => A

  val fileParser: Parser[List[A]] =
    prefixTo(":")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("de_label"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("en_label"))
      .skip(optional(newline))
      .many()
      .map(_.map(makeType))
}
