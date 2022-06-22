package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.People
import parsing.{stringForKey, withFile0}

import javax.inject.Singleton

trait PeopleParser {
  val fileParser: Parser[List[People]]
  val parser: Parser[List[People]]
}

@Singleton
final class PeopleParserImpl(path: String) extends PeopleParser {

  def string(key: String): Parser[String] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(prefix(_ != '\n').or(rest))
      .map(_.trim)

  val fileParser =
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
      .map(_.map(People.tupled))

  val people: List[People] =
    withFile0(path)(s => fileParser.parse(s)._1)
      .fold(
        e => throw e,
        xs =>
          if (xs.isEmpty)
            throw new Throwable("people should not be empty")
          else xs
      )

  val parser = {
    val single =
      oneOf(
        people.map(p =>
          literal(s"person.${p.abbrev}")
            .skip(newline)
            .map(_ => p)
        )
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
