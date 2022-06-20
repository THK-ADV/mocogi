package parsing.helper

import parser.Parser
import parser.Parser._
import parser.ParserOps._

trait MultipleValueParser[A] {
  def multipleParser(key: String, singleParser: Parser[A], minimum: Int = 0): Parser[List[A]] = {
    val dashes =
      zeroOrMoreSpaces
        .skip(prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(singleParser)
        .many(minimum = minimum)

    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .take(singleParser.map(a => List(a)) or dashes)
  }
}
