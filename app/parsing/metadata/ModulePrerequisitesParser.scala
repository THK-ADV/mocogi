package parsing.metadata

import java.util.UUID

import models.ModulePrerequisiteEntryProtocol
import models.ModulePrerequisitesProtocol
import parser.Parser
import parser.Parser.*
import parser.ParserOps.*
import parsing.multipleValueParser
import parsing.stringForKey
import parsing.types.ParsedPrerequisiteEntry
import parsing.types.ParsedPrerequisites
import parsing.uuidParser

object ModulePrerequisitesParser {

  def recommendedKey = "recommended_prerequisites"
  def requiredKey    = "required_prerequisites"
  def modulesKey     = "modules"
  def modulesPrefix  = "module."
  def textKey        = "text"

  private def textParser: Parser[String] =
    stringForKey(textKey).option
      .map(_.getOrElse(""))

  private def modulesParser: Parser[List[UUID]] =
    multipleValueParser(
      modulesKey,
      skipFirst(prefix(modulesPrefix)).take(prefixTo("\n").or(rest)).flatMap(uuidParser)
    ).option.map(_.getOrElse(Nil))

  private def parser(key: String): Parser[ParsedPrerequisiteEntry] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(textParser)
      .skip(zeroOrMoreSpaces)
      .zip(modulesParser)
      .map(ParsedPrerequisiteEntry.apply)

  private def raw(key: String): Parser[ModulePrerequisiteEntryProtocol] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(textParser)
      .skip(zeroOrMoreSpaces)
      .zip(modulesParser)
      .map(ModulePrerequisiteEntryProtocol.apply.tupled)

  def recommendedPrerequisitesParser: Parser[ParsedPrerequisiteEntry] =
    parser(recommendedKey)

  def requiredPrerequisitesParser: Parser[ParsedPrerequisiteEntry] =
    parser(requiredKey)

  def parser =
    recommendedPrerequisitesParser.option
      .skip(zeroOrMoreSpaces)
      .zip(requiredPrerequisitesParser.option)
      .skip(zeroOrMoreSpaces)
      .map(ParsedPrerequisites.apply)

  def recommendedPrerequisitesParserRaw: Parser[ModulePrerequisiteEntryProtocol] =
    raw(recommendedKey)

  def requiredPrerequisitesParserRaw: Parser[ModulePrerequisiteEntryProtocol] =
    raw(requiredKey)

  def raw =
    recommendedPrerequisitesParserRaw.option
      .zip(requiredPrerequisitesParserRaw.option)
      .skip(optional(newline))
      .map(ModulePrerequisitesProtocol.apply.tupled)
}
