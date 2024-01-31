package parsing.core

import models.core.FocusArea
import parser.Parser
import parser.Parser.{newline, optional, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.{P0, P2, P3, P4, P5}
import parsing._

object FocusAreaFileParser {
  def fileParser(implicit
      programs: Seq[String]
  ): Parser[List[FocusArea]] =
    removeIndentation()
      .take(
        optional(singleLineCommentParser())
          .take(prefixTo(":"))
          .skip(newline)
          .skip(zeroOrMoreSpaces)
          .zip(
            singleValueParser[String](
              "program",
              p => s"program.$p"
            )(programs.sorted.reverse)
          )
          .skip(zeroOrMoreSpaces)
          .take(singleLineStringForKey("de_label"))
          .skip(zeroOrMoreSpaces)
          .take(singleLineStringForKey("en_label").option.map(_.getOrElse("")))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("de_desc").option.map(_.getOrElse("")))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("en_desc").option.map(_.getOrElse("")))
          .map((FocusArea.apply _).tupled)
          .all(zeroOrMoreSpaces)
      )
}
