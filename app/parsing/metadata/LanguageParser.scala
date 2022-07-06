package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser3
import parsing.types.Language

import javax.inject.Singleton

@Singleton
final class LanguageParser extends SimpleFileParser3[Language] {
  def parser(implicit languages: Seq[Language]): Parser[Language] =
    makeTypeParser("language", languages, x => s"lang.${x.abbrev}")
}
