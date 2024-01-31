package parsing.metadata

import models.core.{PO, Specialization}
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.{POMandatory, ParsedPOOptional}
import parsing.{multipleValueParser, uuidParser}

object POParser {
  def studyProgramParser(implicit
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[(PO, Option[Specialization])] = {
    val pos0 = pos.sortBy(_.program).reverse
    val specializations0 = specializations.sortBy(_.id).reverse
    val poParser = oneOf(
      pos0.map(s => prefix(s"study_program.${s.id}").map(_ => s)): _*
    )
    val specializationsParser: Parser[Option[Specialization]] =
      (char.map(_.toString) or Parser.rest)
        .flatMap { c =>
          if (c == ".")
            oneOf(specializations0.map(s => prefix(s.id).map(_ => s)): _*)
              .map(Some.apply)
          else always(None)
        }
    prefix("- study_program:")
      .skip(zeroOrMoreSpaces)
      .take(poParser)
      .zip(specializationsParser)
  }

  private def recommendedSemesterParser =
    multipleValueParser("recommended_semester", int).option.map(
      _.getOrElse(Nil)
    )

  private def recommendedSemesterPartTimeParser =
    multipleValueParser("recommended_semester_part_time", int).option.map(
      _.getOrElse(Nil)
    )

  private def instanceOfParser =
    prefix("instance_of:")
      .skip(zeroOrMoreSpaces)
      .take(prefix("module.").take(prefixTo("\n").or(rest)))
      .flatMap(uuidParser)

  private def partOfCatalogParser =
    prefix("part_of_catalog:")
      .skip(zeroOrMoreSpaces)
      .take(boolean)

  def mandatoryPOParser(implicit
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[List[POMandatory]] =
    prefix("po_mandatory:")
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser
          .skip(zeroOrMoreSpaces)
          .zip(recommendedSemesterParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterPartTimeParser)
          .many(zeroOrMoreSpaces)
          .map(_.map { case ((po, spec), recSem, recSemPart) =>
            POMandatory(po, spec, recSem, recSemPart)
          })
      )

  def optionalPOParser(implicit
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[List[ParsedPOOptional]] =
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
          .map(_.map { case ((po, spec), io, cat, recSem) =>
            ParsedPOOptional(po, spec, io, cat, recSem)
          })
      )
}
