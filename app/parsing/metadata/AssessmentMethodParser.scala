package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.helper.{MultipleValueParser, SimpleFileParser}
import parsing.types.{AssessmentMethod, AssessmentMethodPercentage}

import javax.inject.Singleton

trait AssessmentMethodParser {
  val fileParser: Parser[List[AssessmentMethod]]
  val parser: Parser[List[AssessmentMethodPercentage]]
}

@Singleton
final class AssessmentMethodParserImpl(val path: String)
    extends AssessmentMethodParser
    with SimpleFileParser[AssessmentMethod]
    with MultipleValueParser[AssessmentMethodPercentage] {

  override val makeType = AssessmentMethod.tupled
  override val typename = "assessment methods"

  val fileParser: Parser[List[AssessmentMethod]] =
    makeFileParser

  val assessmentMethods: List[AssessmentMethod] = parseTypes

  val parser: Parser[List[AssessmentMethodPercentage]] =
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
        )
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
