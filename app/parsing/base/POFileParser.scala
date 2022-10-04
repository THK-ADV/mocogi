package parsing.base

import basedata.{PO, StudyProgramPreview}
import parser.Parser.{newline, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.{P2, P3, P4, P5, P6}
import parsing._

object POFileParser {
  def fileParser(implicit
      programs: Seq[StudyProgramPreview]
  ) =
    prefixTo(":")
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
        singleValueParser[StudyProgramPreview](
          "program",
          p => s"program.${p.abbrev}"
        )
      )
      .map(PO.tupled)
      .many(zeroOrMoreSpaces)
}
