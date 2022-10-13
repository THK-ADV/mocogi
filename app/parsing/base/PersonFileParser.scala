package parsing.base

import basedata.{Faculty, Person}
import parser.Parser
import parser.Parser.{newline, prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.{P2, P3, P4}
import parsing.{singleLineStringForKey, singleValueParser}

import javax.inject.Singleton

@Singleton
class PersonFileParser {

  def facultyParser(implicit faculties: Seq[Faculty]): Parser[Faculty] =
    singleValueParser("faculty", f => s"faculty.${f.abbrev}")

  def fileParser(implicit faculties: Seq[Faculty]): Parser[List[Person]] =
    prefixTo(":")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("lastname"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("firstname"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("title"))
      .skip(zeroOrMoreSpaces)
      .take(facultyParser)
      .many()
      .map(_.map(Person.tupled))
}
