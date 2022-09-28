package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.types.{ECTS, ECTSFocusAreaContribution, FocusArea}
import parsing.{doubleForKey, removeIndentation, stringForKey}

object ECTSParser {

  def ectsValueParser =
    doubleForKey("ects")

  def ectsContributionsToFocusAreasParser(implicit
      focusAreas: Seq[FocusArea]
  ) = {
    val focusAreaParser: Parser[ECTSFocusAreaContribution] =
      oneOf(
        focusAreas.map { f =>
          prefix(f.abbrev)
            .skip(prefix(":"))
            .skip(zeroOrMoreSpaces)
            .skip(prefix("num:"))
            .skip(zeroOrMoreSpaces)
            .take(double)
            .skip(zeroOrMoreSpaces)
            .zip(stringForKey("desc"))
            .map { case (value, desc) =>
              ECTSFocusAreaContribution(f, value, desc)
            }
        }: _*
      )

    skipFirst(prefix("ects:"))
      .skip(prefixTo("contributions_to_focus_areas:"))
      .skip(zeroOrMoreSpaces)
      .skip(removeIndentation(3))
      .take(focusAreaParser.many())
  }

  def ectsParser(implicit focusAreas: Seq[FocusArea]): Parser[ECTS] = {
    oneOf(
      ectsValueParser.map(d => ECTS(d, Nil)),
      ectsContributionsToFocusAreasParser.map { c => ECTS(0, c) }
    )
  }
}
