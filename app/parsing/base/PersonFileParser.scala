package parsing.base

import basedata.{Faculty, Person, PersonStatus}
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P2, P3, P4, P5, P6}
import parsing.{multipleValueParser, singleLineStringForKey}

import javax.inject.Singleton

@Singleton
class PersonFileParser {

  def unknownParser: Parser[Person] =
    literal("nn")
      .skip(prefix(":"))
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("title"))
      .map(Person.Unknown.tupled)

  def groupsParser: Parser[Person] =
    prefixTo(":")
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("title"))
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

  def singleParser(implicit
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
      .take(statusParser)
      .map(Person.Single.tupled)

  def parser(implicit faculties: Seq[Faculty]): Parser[List[Person]] =
    oneOf(
      unknownParser,
      groupsParser,
      singleParser
    ).all(zeroOrMoreSpaces)
}
