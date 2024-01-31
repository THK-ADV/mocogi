package parsing.metadata

import models.core.Status
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class StatusParser extends SingleValueParser[Status] {
  def parser(implicit status: Seq[Status]): Parser[Status] =
    itemParser(
      "status",
      status.sortBy(_.id).reverse,
      x => s"status.${x.id}"
    )
}
