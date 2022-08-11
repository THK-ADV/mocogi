package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser

object POParser extends MultipleValueParser[String] {
  private def poParser(key: String): Parser[List[String]] =
    multipleParser(
      key,
      skipFirst(not(prefix("-")))
        .take(prefixTo("\n") or rest)
    )

  val mandatoryPOParser: Parser[List[String]] =
    poParser("po_mandatory")

  val optionalPOParser: Parser[List[String]] =
    poParser("po_optional")
}
