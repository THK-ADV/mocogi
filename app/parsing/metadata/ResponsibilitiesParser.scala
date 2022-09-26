package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.{Person, Responsibilities}

import javax.inject.{Inject, Singleton}

@Singleton
class ResponsibilitiesParser @Inject() (personParser: PersonParser) {

  def parser(implicit persons: Seq[Person]): Parser[Responsibilities] = {
    val parser0 = personParser.parser(persons)
    prefix("responsibilities:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .skip(prefix("module_management:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .take(parser0)
      .skip(zeroOrMoreSpaces)
      .skip(prefix("lecturers:"))
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .skip(zeroOrMoreSpaces)
      .zip(parser0)
      .map(Responsibilities.tupled)
  }
}
