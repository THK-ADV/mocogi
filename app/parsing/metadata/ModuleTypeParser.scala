package parsing.metadata

import models.core.ModuleType
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class ModuleTypeParser extends SingleValueParser[ModuleType] {
  def parser(implicit moduleTypes: Seq[ModuleType]): Parser[ModuleType] =
    itemParser(
      "type",
      moduleTypes.sortBy(_.abbrev).reverse,
      x => s"type.${x.abbrev}"
    )
}
