package parsing.metadata

import models.core.Identity
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.Responsibilities

import javax.inject.{Inject, Singleton}

@Singleton
class ResponsibilitiesParser @Inject() (identityParser: IdentityParser) {

  def parser(implicit identities: Seq[Identity]): Parser[Responsibilities] = {
    val parser0 = identityParser.parser(identities)
    prefix("responsibilities:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .skip(prefix("module_management:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .take(parser0)
      .skip(zeroOrMoreSpaces)
      .skip(prefix("lecturers:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .zip(parser0)
      .map((Responsibilities.apply _).tupled)
  }
}
