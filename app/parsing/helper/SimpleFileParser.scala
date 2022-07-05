package parsing.helper

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.{stringForKey, withFile0}

trait SimpleFileParser[A] {
  protected def makeType: ((String, String, String)) => A
  protected def path: String
  protected def typename: String

  protected def makeFileParser: Parser[List[A]] =
    prefixTo(":")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(stringForKey("de_label"))
      .skip(zeroOrMoreSpaces)
      .take(stringForKey("en_label"))
      .skip(optional(newline))
      .many()
      .map(_.map(makeType))

  protected def parseTypes: List[A] =
    withFile0(path)(s => makeFileParser.parse(s)._1)
      .fold(
        e => throw e,
        xs =>
          if (xs.isEmpty)
            throw new Throwable(s"$typename should not be empty")
          else xs
      )

  protected def makeTypeParser(key: String)(lit: A => String): Parser[A] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        oneOf(
          parseTypes.map(m =>
            literal(lit(m))
              .skip(newline)
              .map(_ => m)
          )
        )
      )
}
