package parsing.metadata

import basedata.Status
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class StatusParser extends SingleValueParser[Status] {
  def parser(implicit status: Seq[Status]): Parser[Status] =
    itemParser("status", status, x => s"status.${x.abbrev}")
}
