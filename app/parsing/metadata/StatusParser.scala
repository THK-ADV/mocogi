package parsing.metadata

import parser.Parser
import parsing.helper.SingleValueParser
import parsing.types.Status

import javax.inject.Singleton

@Singleton
final class StatusParser extends SingleValueParser[Status] {
  def parser(implicit status: Seq[Status]): Parser[Status] =
    itemParser("status", status, x => s"status.${x.abbrev}")
}
