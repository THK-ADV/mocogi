package parsing.metadata

import models.core.Identity
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.ModuleResponsibilities

import javax.inject.{Inject, Singleton}

@Singleton
class ModuleResponsibilitiesParser @Inject() (identityParser: IdentityParser) {

  def parser(implicit
      identities: Seq[Identity]
  ): Parser[ModuleResponsibilities] = {
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
      .map((ModuleResponsibilities.apply _).tupled)
  }
}
