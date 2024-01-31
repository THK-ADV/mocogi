package parsing.core

import models.core.PO
import parser.Parser.{newline, optional, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.{P0, P2, P3, P4, P5, P6}
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
      .take(dateForKey("date"))
      .skip(zeroOrMoreSpaces)
      .take(dateForKey("date_from"))
      .skip(zeroOrMoreSpaces)
      .take(dateForKey("date_to").option)
      .skip(zeroOrMoreSpaces)
      .take(
        multipleValueParser(
          "modification_dates",
          prefixTo("\n").flatMap(localDateParser)
        ).option.map(_.getOrElse(Nil))
      )
      .skip(zeroOrMoreSpaces)
      .take(
        singleValueParser[String](
          "program",
          p => s"program.$p"
        )(programs.sorted.reverse)
      )
      .skip(zeroOrMoreSpaces)
      .map((PO.apply _).tupled)
      .all()
}
