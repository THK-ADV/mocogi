package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2}
import parsing.helper.MultipleValueParser.multipleParser
import parsing.types.{AssessmentMethod, AssessmentMethodEntry}

object AssessmentMethodParser {

  private def assessmentMethodParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[AssessmentMethod] =
    oneOf(
      assessmentMethods
        .map(a => prefix(s"assessment.${a.abbrev}").map(_ => a)): _*
    )

  private def methodParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[AssessmentMethod] =
    prefix("- method:")
      .skip(zeroOrMoreSpaces)
      .take(assessmentMethodParser)

  private def percentageParser: Parser[Option[Double]] =
    prefix("percentage:")
      .skip(zeroOrMoreSpaces)
      .take(double)
      .option

  private def preconditionParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[AssessmentMethod]] =
    multipleParser("precondition", assessmentMethodParser, 1).option
      .map(_.getOrElse(Nil))

  private def parser(key: String)(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[AssessmentMethodEntry]] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        methodParser
          .skip(zeroOrMoreSpaces)
          .zip(percentageParser)
          .skip(zeroOrMoreSpaces)
          .take(preconditionParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(AssessmentMethodEntry.tupled))
      )

  def assessmentMethodsMandatoryParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[AssessmentMethodEntry]] =
    parser("assessment_methods_mandatory")

  def assessmentMethodsOptionalParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[AssessmentMethodEntry]] =
    parser("assessment_methods_optional")
}
