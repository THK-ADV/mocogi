package parsing.core

import models.core.IDLabelDesc
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2, P3, P4}
import parsing.{removeIndentation, singleLineStringForKey, stringForKey}

trait LabelDescFileParser[A <: IDLabelDesc] extends FileParser[A] {
  protected def makeType: ((String, String, String, String, String)) => A

  val fileParser: Parser[List[A]] = {
    skipFirst(removeIndentation())
      .take(
        prefixTo(":")
          .skip(newline)
          .skip(zeroOrMoreSpaces)
          .zip(singleLineStringForKey("de_label"))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("de_desc"))
          .skip(zeroOrMoreSpaces)
          .take(singleLineStringForKey("en_label"))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("en_desc"))
          .skip(optional(newline))
          .all()
          .map(_.map(makeType))
      )
  }
}
