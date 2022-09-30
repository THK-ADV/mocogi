package parsing.metadata

import basedata.Person
import parser.Parser
import parser.Parser._
import parser.ParserOps._

import javax.inject.Singleton

@Singleton
final class PersonParser {

  def parser(implicit person: Seq[Person]): Parser[List[Person]] = {
    val single =
      oneOf(
        person.map(p =>
          literal(s"person.${p.abbrev}")
            .skip(newline)
            .map(_ => p)
        ): _*
      )

    val dashes =
      skipFirst(zeroOrMoreSpaces)
        .skip(prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(single)
        .many()

    single.map(a => List(a)) or dashes
  }
}
