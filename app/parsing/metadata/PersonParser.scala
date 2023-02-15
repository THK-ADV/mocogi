package parsing.metadata

import models.core.Person
import parser.Parser
import parser.Parser._
import parser.ParserOps._

import javax.inject.Singleton

@Singleton
final class PersonParser {

  def parser(implicit person: Seq[Person]): Parser[List[Person]] = {
    val single =
      oneOf(
        person
          .sortBy(_.id)
          .reverse
          .map(p =>
            literal(s"person.${p.id}")
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
