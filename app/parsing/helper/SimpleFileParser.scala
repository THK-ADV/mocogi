package parsing.helper

import parser.Parser
import parser.Parser.{newline, optional, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.P2
import parsing.metadata.file.FileParser
import parsing.stringForKey

trait SimpleFileParser[A] extends FileParser[A] {
  protected def makeType: ((String, String, String)) => A

  val fileParser: Parser[List[A]] =
    prefixTo(":")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(stringForKey("de_label"))
      .skip(zeroOrMoreSpaces)
      .take(stringForKey("en_label"))
      .skip(optional(newline))
      .many()
      .map(_.map(makeType))
}
