package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileItemParser
import parsing.types.Language

import javax.inject.Singleton

@Singleton
final class LanguageParser extends SimpleFileItemParser[Language] {
  def parser(implicit languages: Seq[Language]): Parser[Language] =
    itemParser("language", languages, x => s"lang.${x.abbrev}")
}
