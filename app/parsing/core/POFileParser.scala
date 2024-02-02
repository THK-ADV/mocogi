package parsing.core

import models.core.PO
import parser.Parser._
import parser.ParserOps.{P0, P2, P3, P4}
import parsing._

object POFileParser {

  def fileParser(implicit
      programs: Seq[String]
  ) =
    optional(singleLineCommentParser())
      .take(prefixTo(":"))
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(posIntForKey("version"))
      .skip(zeroOrMoreSpaces)
      .skip(dateForKey("date"))
      .skip(zeroOrMoreSpaces)
      .take(dateForKey("date_from"))
      .skip(zeroOrMoreSpaces)
      .take(dateForKey("date_to").option)
      .skip(zeroOrMoreSpaces)
      .skip(optional(skipFirst(range("modification_dates:", "program:"))))
      .skip(zeroOrMoreSpaces)
      .take(
        singleValueParser[String](
          "program",
          p => s"program.$p"
        )(programs.sorted.reverse)
      )
      .skip(zeroOrMoreSpaces)
      .map { case (id, version, dateFrom, dateTo, studyProgram) =>
        PO(id, version, studyProgram, dateFrom, dateTo)
      }
      .all()
}
