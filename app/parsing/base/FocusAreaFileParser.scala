package parsing.base

import basedata.{FocusArea, StudyProgramPreview}
import parser.Parser
import parser.Parser.{newline, optional, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.{P0, P2, P3, P4, P5}
import parsing._

object FocusAreaFileParser {
  def fileParser(implicit
      programs: Seq[StudyProgramPreview]
  ): Parser[List[FocusArea]] =
    removeIndentation()
      .take(
        optional(singleLineCommentParser())
          .take(prefixTo(":"))
          .skip(newline)
          .skip(zeroOrMoreSpaces)
          .zip(
            singleValueParser[StudyProgramPreview](
              "program",
              p => s"program.${p.abbrev}"
            )
          )
          .skip(zeroOrMoreSpaces)
          .take(singleLineStringForKey("de_label"))
          .skip(zeroOrMoreSpaces)
          .take(singleLineStringForKey("en_label").option.map(_.getOrElse("")))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("de_desc").option.map(_.getOrElse("")))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("en_desc").option.map(_.getOrElse("")))
          .map(FocusArea.tupled)
          .many(zeroOrMoreSpaces)
      )
}