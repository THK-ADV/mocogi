package parsing.metadata

import parser.Parser
import parsing.helper.SingleValueParser
import parsing.types.Language

import javax.inject.Singleton

@Singleton
final class LanguageParser extends SingleValueParser[Language] {
  def parser(implicit languages: Seq[Language]): Parser[Language] =
    itemParser("language", languages, x => s"lang.${x.abbrev}")
}
