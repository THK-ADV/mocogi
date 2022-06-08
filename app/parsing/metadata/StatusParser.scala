package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Status

object StatusParser extends SimpleFileParser[Status] {
  override protected val makeType = Status.tupled

  override protected val filename = "status.yaml"

  override protected val typename = "status"

  val statusFileParser: Parser[List[Status]] = fileParser

  val status: List[Status] = types

  val statusParser: Parser[Status] =
    typeParser("status")(t => s"status.${t.abbrev}")
}
