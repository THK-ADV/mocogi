package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser
import parsing.types.{POMandatory, POOptional}

object POParser extends MultipleValueParser[String] {

  private def studyProgramParser =
    prefix("- study_program:")
      .skip(zeroOrMoreSpaces)
      .skip(prefix("study_program."))
      .take(prefixTo("\n"))

  private def recommendedSemesterParser =
    new MultipleValueParser[Int] {}.multipleParser("recommended_semester", int)

  private def recommendedSemesterPartTimeParser =
    new MultipleValueParser[Int] {}
      .multipleParser("recommended_semester_part_time", int)
      .option
      .map(_.getOrElse(Nil))

  private def instanceOfParser =
    prefix("instance_of:")
      .skip(zeroOrMoreSpaces)
      .take(prefixTo("\n"))

  private def partOfCatalogParser =
    prefix("part_of_catalog:")
      .skip(zeroOrMoreSpaces)
      .take(boolean)

  val mandatoryPOParser: Parser[List[POMandatory]] =
    prefix("po_mandatory:")
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser
          .skip(zeroOrMoreSpaces)
          .zip(recommendedSemesterParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterPartTimeParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(POMandatory.tupled))
      )

  val optionalPOParser: Parser[List[POOptional]] =
    prefix("po_optional:")
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser
          .skip(zeroOrMoreSpaces)
          .zip(instanceOfParser)
          .skip(zeroOrMoreSpaces)
          .take(partOfCatalogParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(POOptional.tupled))
      )
}
