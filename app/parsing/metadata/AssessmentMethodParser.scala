package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.helper.MultipleValueParser
import parsing.types.{AssessmentMethod, AssessmentMethodPercentage}

import javax.inject.Singleton

@Singleton
final class AssessmentMethodParser
    extends MultipleValueParser[AssessmentMethodPercentage] {

  def parser(
    implicit assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[AssessmentMethodPercentage]] =
    multipleParser(
      "assessment-methods",
      oneOf(
        assessmentMethods.map(s =>
          literal(s"assessment.${s.abbrev}")
            .zip(
              zeroOrMoreSpaces
                .skip(optional(prefix("(")))
                .skip(zeroOrMoreSpaces)
                .take(double)
                .skip(zeroOrMoreSpaces)
                .skip(optional(prefix("%")))
                .skip(zeroOrMoreSpaces)
                .skip(optional(prefix(")")))
                .option
            )
            .skip(newline)
            .map(res => AssessmentMethodPercentage(s, res._2))
        ): _*
      ),
      1
    )
      .flatMap { xs =>
        if (xs.forall(_.percentage.isDefined))
          isValid(xs).fold(always(xs))(d =>
            never(
              s"percentage of all assessment methods to be 100.0 %, but was $d %"
            )
          )
        else always(xs)
      }

  private def sumPercentages(xs: List[AssessmentMethodPercentage]): Double =
    xs.foldLeft(0.0) { case (acc, x) => acc + x.percentage.get }

  private def isValid(xs: List[AssessmentMethodPercentage]): Option[Double] = {
    val sum = sumPercentages(xs)
    Option.unless(sum == 100.0)(sum)
  }
}
