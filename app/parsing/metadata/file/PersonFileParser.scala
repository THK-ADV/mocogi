package parsing.metadata.file

import basedata.Person
import parser.Parser
import parser.Parser.{newline, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.{P2, P3, P4}
import parsing.singleLineStringForKey

import javax.inject.Singleton

@Singleton
class PersonFileParser extends FileParser[Person] {
  val fileParser: Parser[List[Person]] =
    prefixTo(":")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("lastname"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("firstname"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("title"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("faculty"))
      .many()
      .map(_.map(Person.tupled))
}
