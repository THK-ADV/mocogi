package parsing.metadata

import models.core.ModuleLanguage
import parser.Parser
import parsing.helper.SingleValueParser
import parsing.singleValueRawParser

object ModuleLanguageParser extends SingleValueParser[ModuleLanguage] {
  private def key = "language"
  private def prefix = "lang."

  def parser(implicit languages: Seq[ModuleLanguage]): Parser[ModuleLanguage] =
    itemParser(
      key,
      languages.sortBy(_.id).reverse,
      x => s"$prefix${x.id}"
    )

  def raw: Parser[String] =
    singleValueRawParser(key, prefix)
}
