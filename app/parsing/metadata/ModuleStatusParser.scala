package parsing.metadata

import models.core.ModuleStatus
import parser.Parser
import parsing.helper.SingleValueParser
import parsing.singleValueRawParser

object ModuleStatusParser extends SingleValueParser[ModuleStatus] {
  private def key = "status"
  private def prefix = "status."

  def parser(implicit status: Seq[ModuleStatus]): Parser[ModuleStatus] = {
    itemParser(
      key,
      status.sortBy(_.id).reverse,
      x => s"$prefix${x.id}"
    )
  }

  def raw: Parser[String] =
    singleValueRawParser(key, prefix)
}
