package parsing.metadata

import models.core.AssessmentMethod
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2}
import parsing.multipleValueParser
import parsing.types.ModuleAssessmentMethodEntry

object ModuleAssessmentMethodParser {

  private def assessmentMethodParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[AssessmentMethod] =
    oneOf(
      assessmentMethods
        .map(a => prefix(s"assessment.${a.id}").map(_ => a)): _*
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
    multipleValueParser("precondition", assessmentMethodParser, 1).option
      .map(_.getOrElse(Nil))

  private def parser(key: String)(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[ModuleAssessmentMethodEntry]] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        methodParser
          .skip(zeroOrMoreSpaces)
          .zip(percentageParser)
          .skip(zeroOrMoreSpaces)
          .take(preconditionParser)
          .many(zeroOrMoreSpaces)
          .map(_.map((ModuleAssessmentMethodEntry.apply _).tupled))
      )

  def mandatoryParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[ModuleAssessmentMethodEntry]] =
    parser("assessment_methods_mandatory")(
      assessmentMethods.sortBy(_.id).reverse
    )

  def electiveParser(implicit
      assessmentMethods: Seq[AssessmentMethod]
  ): Parser[List[ModuleAssessmentMethodEntry]] =
    parser("assessment_methods_optional")(
      assessmentMethods.sortBy(_.id).reverse
    )
}