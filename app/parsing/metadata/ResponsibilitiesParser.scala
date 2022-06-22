package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.Responsibilities

import javax.inject.{Inject, Singleton}

trait ResponsibilitiesParser {
  val parser: Parser[Responsibilities]
}

@Singleton
class ResponsibilitiesParserImpl @Inject() (peopleParser: PeopleParser)
    extends ResponsibilitiesParser {
  val parser: Parser[Responsibilities] =
    prefix("responsibilities:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .skip(prefix("coordinator:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .take(peopleParser.parser)
      .skip(zeroOrMoreSpaces)
      .skip(prefix("lecturers:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .zip(peopleParser.parser)
      .map(Responsibilities.tupled)
}
