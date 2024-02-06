package parsing.metadata

import models.core.ModuleStatus
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class ModuleStatusParser extends SingleValueParser[ModuleStatus] {
  def parser(implicit status: Seq[ModuleStatus]): Parser[ModuleStatus] =
    itemParser(
      "status",
      status.sortBy(_.id).reverse,
      x => s"status.${x.id}"
    )
}
