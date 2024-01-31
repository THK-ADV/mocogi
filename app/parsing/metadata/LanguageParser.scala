package parsing.metadata

import models.core.Language
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class LanguageParser extends SingleValueParser[Language] {
  def parser(implicit languages: Seq[Language]): Parser[Language] =
    itemParser(
      "language",
      languages.sortBy(_.id).reverse,
      x => s"lang.${x.id}"
    )
}
