package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser

object POParser extends MultipleValueParser[String] {
  val poParser: Parser[List[String]] =
    multipleParser(
      "po",
      skipFirst(not(prefix("-")))
        .take(prefixTo("\n") or rest)
    )
}
