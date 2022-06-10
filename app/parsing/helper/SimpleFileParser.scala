package parsing.helper

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.{stringForKey, withResFile}

trait SimpleFileParser[A] {
  protected def makeType: ((String, String)) => A
  protected def filename: String
  protected def typename: String

  protected def fileParser: Parser[List[A]] =
    prefixTo(":")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(stringForKey("de_label"))
      .skip(optional(newline))
      .zeroOrMore()
      .map(_.map(makeType))

  protected def types: List[A] =
    withResFile(filename)(s => fileParser.parse(s)._1)
      .fold(
        e => throw e,
        xs =>
          if (xs.isEmpty)
            throw new Throwable(s"$typename should not be empty")
          else xs
      )

  protected def typeParser(key: String)(lit: A => String): Parser[A] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        oneOf(
          types.map(m =>
            literal(lit(m))
              .skip(newline)
              .map(_ => m)
          )
        )
      )
}
