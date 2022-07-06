package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileItemParser
import parsing.types.Status

import javax.inject.Singleton

@Singleton
final class StatusParser extends SimpleFileItemParser[Status] {
  def parser(implicit status: Seq[Status]): Parser[Status] =
    itemParser("status", status, x => s"status.${x.abbrev}")
}
