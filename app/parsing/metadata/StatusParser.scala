package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser3
import parsing.types.Status

import javax.inject.Singleton

@Singleton
final class StatusParser extends SimpleFileParser3[Status] {
  def parser(implicit status: Seq[Status]): Parser[Status] =
    makeTypeParser("status", status, x => s"status.${x.abbrev}")
}
