package parsing.metadata

import basedata.FocusAreaPreview
import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.types.ECTSFocusAreaContribution
import parsing.{doubleForKey, removeIndentation, stringForKey}

object ECTSParser {

  def ectsValueParser =
    doubleForKey("ects")

  def ectsContributionsToFocusAreasParser(implicit
      focusAreas: Seq[FocusAreaPreview]
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

  def ectsParser(implicit
      focusAreas: Seq[FocusAreaPreview]
  ): Parser[Either[Double, List[ECTSFocusAreaContribution]]] = {
    oneOf(
      ectsValueParser.map(Left.apply),
      ectsContributionsToFocusAreasParser.map(Right.apply)
    )
  }
}
