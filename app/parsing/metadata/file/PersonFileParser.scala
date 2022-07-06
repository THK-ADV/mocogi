package parsing.metadata.file

import parser.Parser
import parser.Parser.{newline, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.{P2, P3, P4}
import parsing.stringForKey
import parsing.types.Person

import javax.inject.Singleton

@Singleton
class PersonFileParser extends FileParser[Person] {
  val fileParser: Parser[List[Person]] =
    prefixTo(":")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(stringForKey("lastname"))
      .skip(zeroOrMoreSpaces)
      .take(stringForKey("firstname"))
      .skip(zeroOrMoreSpaces)
      .take(stringForKey("title"))
      .skip(zeroOrMoreSpaces)
      .take(stringForKey("faculty"))
      .many()
      .map(_.map(Person.tupled))
}
