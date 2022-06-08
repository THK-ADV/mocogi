package parsing.metadata

import parser.Parser._
import parser.ParserOps._
import parsing.types.People
import parsing.{stringForKey, withResFile}

object PeopleParser {
  val peopleFileParser =
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
      .zeroOrMore()
      .map(_.map(People.tupled))

  val people: List[People] =
    withResFile("people-all.yaml")(s => peopleFileParser.run(s)._1)
      .fold(
        e => throw e,
        xs =>
          if (xs.isEmpty)
            throw new Throwable("people should not be empty")
          else xs
      )

  val personParser = {
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
        .zeroOrMore()

    single.map(a => List(a)) or dashes
  }
}
