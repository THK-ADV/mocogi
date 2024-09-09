package parsing.metadata

import models.core.ModuleType
import parser.Parser
import parsing.helper.SingleValueParser
import parsing.singleValueRawParser

object ModuleTypeParser extends SingleValueParser[ModuleType] {
  def key = "type"
  def prefix = "type."

  def parser(implicit moduleTypes: Seq[ModuleType]): Parser[ModuleType] =
    itemParser(
      key,
      moduleTypes.sortBy(_.id).reverse,
      x => s"$prefix${x.id}"
    )

  def raw: Parser[String] =
    singleValueRawParser(key, prefix)
}
