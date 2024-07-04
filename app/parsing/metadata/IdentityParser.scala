package parsing.metadata

import cats.data.NonEmptyList
import models.core.Identity
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.ParserListOps

object IdentityParser {
  private def prefix = "person."

  def parser(implicit
      identities: Seq[Identity]
  ): Parser[NonEmptyList[Identity]] = {
    val single =
      oneOf(
        identities
          .sortBy(_.id)
          .reverse
          .map(p =>
            literal(s"$prefix${p.id}")
              .skip(newline)
              .map(_ => p)
          ): _*
      )

    val dashes =
      skipFirst(zeroOrMoreSpaces)
        .skip(Parser.prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(single)
        .many()

    single.map(a => NonEmptyList.one(a)) or dashes.nel()
  }

  def raw: Parser[NonEmptyList[String]] = {
    val single =
      skipFirst(Parser.prefix(prefix))
        .take(prefixTo("\n").or(rest))
        .map(_.trim)

    val dashes =
      skipFirst(zeroOrMoreSpaces)
        .skip(Parser.prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(single)
        .many()

    single.map(a => NonEmptyList.one(a)) or dashes.nel()
  }
}
