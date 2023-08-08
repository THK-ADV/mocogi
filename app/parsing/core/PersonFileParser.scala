package parsing.core

import models.core.{Faculty, Person, PersonStatus}
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P2, P3, P4, P5, P6, P7}
import parsing.{multipleValueParser, singleLineStringForKey}

import javax.inject.Singleton

@Singleton
class PersonFileParser {

  def unknownParser: Parser[Person] =
    literal("nn")
      .skip(prefix(":"))
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("label"))
      .map(Person.Unknown.tupled)

  def groupParser: Parser[Person] =
    prefixTo(":")
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("label").map(s => if (s == "--") "" else s))
      .map(Person.Group.tupled)

  def statusParser: Parser[PersonStatus] =
    singleLineStringForKey("status")
      .map(PersonStatus.apply)

  def facultiesParser(implicit faculties: Seq[Faculty]): Parser[List[Faculty]] =
    multipleValueParser(
      "faculty",
      a => s"faculty.${a.abbrev}",
      1
    )

  def defaultParser(implicit
      faculties: Seq[Faculty]
  ): Parser[Person] =
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
      .map(Person.Default.tupled)

  def parser(implicit faculties: Seq[Faculty]): Parser[List[Person]] =
    oneOf(
      unknownParser,
      groupParser,
      defaultParser
    ).all(zeroOrMoreSpaces)
}
