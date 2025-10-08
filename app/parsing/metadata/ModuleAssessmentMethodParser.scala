package parsing.metadata

import models.core.AssessmentMethod
import models.ModuleAssessmentMethodEntryProtocol
import models.ModuleAssessmentMethodsProtocol
import parser.Parser
import parser.Parser.*
import parser.ParserOps.P0
import parser.ParserOps.P2
import parsing.multipleValueParser
import parsing.types.ModuleAssessmentMethodEntry
import parsing.types.ModuleAssessmentMethods

object ModuleAssessmentMethodParser {

  def assessmentPrefix = "assessment."
  def preconditionKey  = "precondition"
  def mandatoryKey     = "assessment_methods_mandatory"

  private def assessmentMethodParser(implicit assessmentMethods: Seq[AssessmentMethod]): Parser[AssessmentMethod] =
    oneOf(
      assessmentMethods
        .map(a => prefix(s"$assessmentPrefix${a.id}").map(_ => a))*
    )

  private def assessmentMethodParserRaw: Parser[String] =
    skipFirst(prefix(assessmentPrefix))
      .take(prefixTo("\n").or(rest))
      .map(_.trim)

  private def methodParser(implicit assessmentMethods: Seq[AssessmentMethod]): Parser[AssessmentMethod] =
    prefix("- method:")
      .skip(zeroOrMoreSpaces)
      .take(assessmentMethodParser)

  private def methodParserRaw: Parser[String] =
    prefix("- method:")
      .skip(zeroOrMoreSpaces)
      .take(assessmentMethodParserRaw)

  private def percentageParser: Parser[Option[Double]] =
    prefix("percentage:")
      .skip(zeroOrMoreSpaces)
      .take(double)
      .option

  private def preconditionParser(implicit assessmentMethods: Seq[AssessmentMethod]): Parser[List[AssessmentMethod]] =
    multipleValueParser(preconditionKey, assessmentMethodParser).option
      .map(_.getOrElse(Nil))

  private def preconditionParserRaw: Parser[List[String]] = {
    val dashes =
      zeroOrMoreSpaces
        .skip(prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(assessmentMethodParserRaw)
        .many(minimum = 1)
    prefix(s"$preconditionKey:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .take(assessmentMethodParserRaw.map(a => List(a)).or(dashes))
      .option
      .map(_.getOrElse(Nil))
  }

  private def parser(
      key: String
  )(implicit assessmentMethods: Seq[AssessmentMethod]): Parser[List[ModuleAssessmentMethodEntry]] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        methodParser
          .skip(zeroOrMoreSpaces)
          .zip(percentageParser)
          .skip(zeroOrMoreSpaces)
          .take(preconditionParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(ModuleAssessmentMethodEntry.apply.tupled))
      )

  private def raw(
      key: String
  ): Parser[List[ModuleAssessmentMethodEntryProtocol]] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        methodParserRaw
          .skip(zeroOrMoreSpaces)
          .zip(percentageParser)
          .skip(zeroOrMoreSpaces)
          .take(preconditionParserRaw)
          .many(zeroOrMoreSpaces)
          .map(_.map(ModuleAssessmentMethodEntryProtocol.apply.tupled))
      )

  def mandatoryParser(implicit xs: Seq[AssessmentMethod]): Parser[List[ModuleAssessmentMethodEntry]] =
    parser(mandatoryKey)(xs.sortBy(_.id).reverse).option.map(_.getOrElse(Nil))

  def parser(implicit xs: Seq[AssessmentMethod]): Parser[ModuleAssessmentMethods] =
    mandatoryParser.map(ModuleAssessmentMethods.apply)

  def mandatoryParserRaw: Parser[List[ModuleAssessmentMethodEntryProtocol]] =
    raw(mandatoryKey).option.map(_.getOrElse(Nil))

  def raw: Parser[ModuleAssessmentMethodsProtocol] =
    mandatoryParserRaw.map(ModuleAssessmentMethodsProtocol.apply)
}
