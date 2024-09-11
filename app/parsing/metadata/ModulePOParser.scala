package parsing.metadata

import models.core.{PO, Specialization}
import models.{
  ModulePOMandatoryProtocol,
  ModulePOOptionalProtocol,
  ModulePOProtocol
}
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.{ModulePOMandatory, ParsedPOOptional, ParsedPOs}
import parsing.{multipleValueParser, uuidParser}

object ModulePOParser {

  def studyProgramKey = "study_program"
  def studyProgramPrefix = "study_program."
  def modulePOMandatoryKey = "po_mandatory:"
  def modulePOElectiveKey = "po_optional:"
  def instanceOfKey = "instance_of"
  def modulePrefix = "module."
  def partOfCatalogKey = "part_of_catalog"
  def recommendedSemesterKey = "recommended_semester"

  def studyProgramParser(implicit
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[(PO, Option[Specialization])] = {
    val pos0 = pos.sortBy(_.program).reverse
    val specializations0 = specializations.sortBy(_.id).reverse
    val poParser = oneOf(
      pos0.map(s => prefix(s"$studyProgramPrefix${s.id}").map(_ => s)): _*
    )
    val specializationsParser: Parser[Option[Specialization]] =
      (char.map(_.toString) or Parser.rest)
        .flatMap { c =>
          if (c == ".")
            oneOf(specializations0.map(s => prefix(s.id).map(_ => s)): _*)
              .map(Some.apply)
          else always(None)
        }
    prefix(s"- $studyProgramKey:")
      .skip(zeroOrMoreSpaces)
      .take(poParser)
      .zip(specializationsParser)
  }

  def studyProgramParserRaw: Parser[(String, Option[String])] = {
    val poParser = skipFirst(prefix(studyProgramPrefix))
      .take(prefixTo("\n").or(rest))
      .map(_.trim)

    prefix(s"- $studyProgramKey:")
      .skip(zeroOrMoreSpaces)
      .take(poParser)
      .flatMap { po =>
        po.split('.').length match {
          case 1 => always((po.split('.').head, None))
          case 2 => always((po.split('.').head, Some(po.split('.')(1))))
          case _ => never("po and one specialization at most")
        }
      }
  }

  private def recommendedSemesterParser =
    multipleValueParser(recommendedSemesterKey, int).option.map(
      _.getOrElse(Nil)
    )

  private def recommendedSemesterPartTimeParser =
    multipleValueParser("recommended_semester_part_time", int).option.map(
      _.getOrElse(Nil)
    )

  private def instanceOfParser =
    prefix(s"$instanceOfKey:")
      .skip(zeroOrMoreSpaces)
      .take(prefix(modulePrefix).take(prefixTo("\n").or(rest)))
      .flatMap(uuidParser)

  private def partOfCatalogParser =
    prefix(s"${partOfCatalogKey}:")
      .skip(zeroOrMoreSpaces)
      .take(boolean)

  def mandatoryParser(implicit
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[List[ModulePOMandatory]] =
    prefix(modulePOMandatoryKey)
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser
          .skip(zeroOrMoreSpaces)
          .zip(recommendedSemesterParser)
          .skip(zeroOrMoreSpaces)
          .skip(recommendedSemesterPartTimeParser)
          .many(zeroOrMoreSpaces)
          .map(_.map { case ((po, spec), recSem) =>
            ModulePOMandatory(po, spec, recSem)
          })
      )

  def electiveParser(implicit
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[List[ParsedPOOptional]] =
    prefix(modulePOElectiveKey)
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

  def parser(implicit pos: Seq[PO], specializations: Seq[Specialization]) =
    mandatoryParser.option
      .map(_.getOrElse(Nil))
      .zip(electiveParser.option.map(_.getOrElse(Nil)))
      .map(ParsedPOs.tupled)

  def mandatoryParserRaw: Parser[List[ModulePOMandatoryProtocol]] =
    prefix(modulePOMandatoryKey)
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParserRaw
          .skip(zeroOrMoreSpaces)
          .zip(recommendedSemesterParser)
          .skip(zeroOrMoreSpaces)
          .skip(recommendedSemesterPartTimeParser)
          .many(zeroOrMoreSpaces)
          .map(_.map { case ((po, spec), recSem) =>
            ModulePOMandatoryProtocol(po, spec, recSem)
          })
      )

  def electiveParserRaw: Parser[List[ModulePOOptionalProtocol]] =
    prefix(modulePOElectiveKey)
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParserRaw
          .skip(zeroOrMoreSpaces)
          .zip(instanceOfParser)
          .skip(zeroOrMoreSpaces)
          .take(partOfCatalogParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterParser)
          .many(zeroOrMoreSpaces)
          .map(_.map { case ((po, spec), io, cat, recSem) =>
            ModulePOOptionalProtocol(po, spec, io, cat, recSem)
          })
      )

  def raw =
    mandatoryParserRaw.option
      .map(_.getOrElse(Nil))
      .zip(electiveParserRaw.option.map(_.getOrElse(Nil)))
      .map((ModulePOProtocol.apply _).tupled)
}
