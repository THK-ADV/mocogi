package parsing.metadata

import models.core.PO
import models.{ModulePrerequisiteEntryProtocol, ModulePrerequisitesProtocol}
import parser.Parser
import parser.Parser.*
import parser.ParserOps.*
import parsing.types.{ParsedPrerequisiteEntry, ParsedPrerequisites}
import parsing.{
  multipleValueParser,
  removeIndentation,
  stringForKey,
  uuidParser
}

import java.util.UUID

object ModulePrerequisitesParser {

  def recommendedKey = "recommended_prerequisites"
  def requiredKey = "required_prerequisites"
  def studyProgramsKey = "study_programs"
  def modulesKey = "modules"
  def studyProgramsPrefix = "study_program."
  def modulesPrefix = "module."
  def textKey = "text"

  private def textParser: Parser[String] =
    stringForKey(textKey).option
      .map(_.getOrElse(""))

  private def modulesParser: Parser[List[UUID]] =
    multipleValueParser(
      modulesKey,
      skipFirst(prefix(modulesPrefix)).take(prefixTo("\n")).flatMap(uuidParser)
    ).option.map(_.getOrElse(Nil))

  private def studyProgramsParser(implicit pos: Seq[PO]): Parser[List[PO]] =
    multipleValueParser(
      studyProgramsKey,
      oneOf(
        pos.map(s =>
          literal(s"$studyProgramsPrefix${s.id}")
            .map(_ => s)
        ): _*
      )
    ).option.map(_.getOrElse(Nil))

  private def studyProgramsParserRaw: Parser[List[String]] = {
    val singleParser = skipFirst(prefix(studyProgramsPrefix))
      .take(prefixTo("\n").or(rest))
      .map(_.trim)
    val dashes =
      zeroOrMoreSpaces
        .skip(prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(singleParser)
        .many()
    prefix(s"$studyProgramsKey:")
      .skip(zeroOrMoreSpaces)
      .take(singleParser.map(a => List(a)) or dashes)
      .option
      .map(_.getOrElse(Nil))
  }

  private def parser(
      key: String
  )(implicit pos: Seq[PO]): Parser[ParsedPrerequisiteEntry] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(removeIndentation())
      .take(textParser)
      .zip(modulesParser)
      .take(studyProgramsParser(pos.sortBy(_.id).reverse))
      .map(ParsedPrerequisiteEntry.apply)

  private def raw(key: String): Parser[ModulePrerequisiteEntryProtocol] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(removeIndentation())
      .take(textParser)
      .zip(modulesParser)
      .take(studyProgramsParserRaw)
      .map((ModulePrerequisiteEntryProtocol.apply _).tupled)

  def recommendedPrerequisitesParser(implicit
      xs: Seq[PO]
  ): Parser[ParsedPrerequisiteEntry] =
    parser(recommendedKey)

  def requiredPrerequisitesParser(implicit
      xs: Seq[PO]
  ): Parser[ParsedPrerequisiteEntry] =
    parser(requiredKey)

  def parser(implicit xs: Seq[PO]) =
    recommendedPrerequisitesParser.option
      .skip(zeroOrMoreSpaces)
      .zip(requiredPrerequisitesParser.option)
      .skip(zeroOrMoreSpaces)
      .map(ParsedPrerequisites.apply)

  def recommendedPrerequisitesParserRaw
      : Parser[ModulePrerequisiteEntryProtocol] =
    raw(recommendedKey)

  def requiredPrerequisitesParserRaw: Parser[ModulePrerequisiteEntryProtocol] =
    raw(requiredKey)

  def raw =
    recommendedPrerequisitesParserRaw.option
      .zip(requiredPrerequisitesParserRaw.option)
      .skip(optional(newline))
      .map((ModulePrerequisitesProtocol.apply _).tupled)
}
