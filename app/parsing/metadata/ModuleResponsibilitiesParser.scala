package parsing.metadata

import cats.data.NonEmptyList
import models.core.Identity
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.ModuleResponsibilities

object ModuleResponsibilitiesParser {

  def key = "responsibilities"

  def moduleManagementKey = "module_management"

  def lecturersKey = "lecturers"

  private def inner[A](
      identityParser: Parser[NonEmptyList[A]]
  ): Parser[(NonEmptyList[A], NonEmptyList[A])] = {
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .skip(prefix(s"$moduleManagementKey:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .take(identityParser)
      .skip(zeroOrMoreSpaces)
      .skip(prefix(s"$lecturersKey:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .zip(identityParser)
  }

  def parser(implicit identities: Seq[Identity]): Parser[ModuleResponsibilities] =
    inner(IdentityParser.parser(identities))
      .map(ModuleResponsibilities.apply.tupled)

  def raw: Parser[(NonEmptyList[String], NonEmptyList[String])] =
    inner(IdentityParser.raw)
}
