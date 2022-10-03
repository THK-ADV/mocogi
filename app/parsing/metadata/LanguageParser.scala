package parsing.metadata

import basedata.Language
import parser.Parser
import parsing.helper.SingleValueParser

import javax.inject.Singleton

@Singleton
final class LanguageParser extends SingleValueParser[Language] {
  def parser(implicit languages: Seq[Language]): Parser[Language] =
    itemParser("language", languages, x => s"lang.${x.abbrev}")
}
