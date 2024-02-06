package parsing.metadata

import models.core.FocusAreaID
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2}
import parsing.types.ModuleECTSFocusAreaContribution
import parsing.{doubleForKey, removeIndentation, stringForKey}

object ModuleECTSParser {

  def ectsValueParser =
    doubleForKey("ects")

  def ectsContributionsToFocusAreasParser(implicit
      focusAreas: Seq[FocusAreaID]
  ) = {
    val focusAreaParser: Parser[ModuleECTSFocusAreaContribution] =
      oneOf(
        focusAreas.map { f =>
          prefix(f.id)
            .skip(prefix(":"))
            .skip(zeroOrMoreSpaces)
            .skip(prefix("num:"))
            .skip(zeroOrMoreSpaces)
            .take(double)
            .skip(zeroOrMoreSpaces)
            .zip(stringForKey("de_desc"))
            .skip(zeroOrMoreSpaces)
            .take(stringForKey("en_desc").option.map(_.getOrElse("")))
            .map { case (value, deDesc, enDesc) =>
              ModuleECTSFocusAreaContribution(f, value, deDesc, enDesc)
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
      focusAreas: Seq[FocusAreaID]
  ): Parser[Either[Double, List[ModuleECTSFocusAreaContribution]]] = {
    oneOf(
      ectsValueParser.map(Left.apply),
      ectsContributionsToFocusAreasParser(focusAreas.sortBy(_.id).reverse)
        .map(Right.apply)
    )
  }
}
