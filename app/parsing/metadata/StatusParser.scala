package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Status

import javax.inject.Singleton

trait StatusParser {
  val fileParser: Parser[List[Status]]
  val parser: Parser[Status]
}

@Singleton
final class StatusParserImpl(val path: String)
    extends StatusParser
    with SimpleFileParser[Status] {

  override protected val makeType = Status.tupled
  override protected val typename = "status"

  val fileParser: Parser[List[Status]] = makeFileParser

  val status: List[Status] = parseTypes

  val parser: Parser[Status] =
    makeTypeParser("status")(t => s"status.${t.abbrev}")
}
