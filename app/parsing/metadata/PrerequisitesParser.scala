package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser

object PrerequisitesParser extends MultipleValueParser[String] {

  private def parser(key: String): Parser[List[String]] = oneOf(
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(prefix("none"))
      .skip(newline)
      .map(_ => Nil),
    multipleParser(
      s"$key",
      skipFirst(prefix("module."))
        .take(prefixTo("\n"))
    )
  )

  val recommendedPrerequisitesParser: Parser[List[String]] = parser(
    "recommended-prerequisites"
  )

  val requiredPrerequisitesParser: Parser[List[String]] = parser(
    "required-prerequisites"
  )
}
