package parsing.metadata

import models.core.ModuleLanguage
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class ModuleLanguageParser extends SingleValueParser[ModuleLanguage] {
  def parser(implicit languages: Seq[ModuleLanguage]): Parser[ModuleLanguage] =
    itemParser(
      "language",
      languages.sortBy(_.id).reverse,
      x => s"lang.${x.id}"
    )
}
