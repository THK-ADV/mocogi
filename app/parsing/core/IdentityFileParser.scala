package parsing.core

import models.core.{Faculty, Identity, PersonStatus}
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P2, P3, P4, P5, P6, P7}
import parsing.{multipleValueParser, singleLineStringForKey}

object IdentityFileParser {

  def unknownParser: Parser[Identity] =
    literal("nn")
      .skip(prefix(":"))
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("label"))
      .map(Identity.Unknown.tupled)

  def groupParser: Parser[Identity] =
    prefixTo(":")
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("label").map(s => if (s == "--") "" else s))
      .map(Identity.Group.tupled)

  def statusParser: Parser[PersonStatus] =
    singleLineStringForKey("status")
      .map(PersonStatus.apply)

  def facultiesParser(implicit faculties: Seq[Faculty]): Parser[List[Faculty]] =
    multipleValueParser(
      "faculty",
      a => s"faculty.${a.id}",
      1
    )

  def personParser(implicit
      faculties: Seq[Faculty]
  ): Parser[Identity] =
    prefixTo(":")
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("lastname"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("firstname"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("title"))
      .skip(zeroOrMoreSpaces)
      .take(facultiesParser)
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("abbreviation"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("campusid"))
      .skip(zeroOrMoreSpaces)
      .take(statusParser)
      .map(Identity.Person.tupled)

  def parser(implicit faculties: Seq[Faculty]): Parser[List[Identity]] =
    oneOf(
      unknownParser,
      groupParser,
      personParser
    ).all(zeroOrMoreSpaces)
}
