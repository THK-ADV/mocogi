package parsing.metadata

import basedata.PO
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.{POMandatory, ParsedPOOptional}
import parsing.{multipleValueParser, uuidParser}

object POParser {
  private def studyProgramParser(implicit pos: Seq[PO]): Parser[PO] =
    prefix("- study_program:")
      .skip(zeroOrMoreSpaces)
      .take(
        oneOf(
          pos.map(s =>
            prefix(s"study_program.${s.abbrev}")
              .map(_ => s)
          ): _*
        )
      )

  private def recommendedSemesterParser =
    multipleValueParser("recommended_semester", int)

  private def recommendedSemesterPartTimeParser =
    multipleValueParser("recommended_semester_part_time", int).option
      .map(_.getOrElse(Nil))

  private def instanceOfParser =
    prefix("instance_of:")
      .skip(zeroOrMoreSpaces)
      .take(prefix("module.").take(prefixTo("\n").or(rest)))
      .flatMap(uuidParser)

  private def partOfCatalogParser =
    prefix("part_of_catalog:")
      .skip(zeroOrMoreSpaces)
      .take(boolean)

  def mandatoryPOParser(implicit pos: Seq[PO]): Parser[List[POMandatory]] =
    prefix("po_mandatory:")
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser(pos.sortBy(_.program).reverse)
          .skip(zeroOrMoreSpaces)
          .zip(recommendedSemesterParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterPartTimeParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(POMandatory.tupled))
      )

  def optionalPOParser(implicit pos: Seq[PO]): Parser[List[ParsedPOOptional]] =
    prefix("po_optional:")
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser(pos.sortBy(_.program).reverse)
          .skip(zeroOrMoreSpaces)
          .zip(instanceOfParser)
          .skip(zeroOrMoreSpaces)
          .take(partOfCatalogParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(ParsedPOOptional.tupled))
      )
}
