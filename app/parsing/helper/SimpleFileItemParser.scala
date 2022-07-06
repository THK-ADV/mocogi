package parsing.helper

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0

trait SimpleFileItemParser[A] {
  def itemParser(
      key: String,
      types: Seq[A],
      lit: A => String
  ): Parser[A] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        oneOf(
          types.map(m =>
            literal(lit(m))
              .skip(newline)
              .map(_ => m)
          ): _*
        )
      )
}
