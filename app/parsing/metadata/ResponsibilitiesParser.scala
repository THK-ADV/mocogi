package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.metadata.PeopleParser.personParser
import parsing.types.Responsibilities

object ResponsibilitiesParser {
  val responsibilitiesParser: Parser[Responsibilities] =
    prefix("responsibilities:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .skip(prefix("coordinator:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .take(personParser)
      .skip(zeroOrMoreSpaces)
      .skip(prefix("lecturers:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .zip(personParser)
      .map(Responsibilities.tupled)
}
