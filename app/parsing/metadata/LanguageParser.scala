package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Language

object LanguageParser extends SimpleFileParser[Language] {

  override val makeType = Language.tupled

  override val filename = "lang.yaml"

  override val typename = "languages"

  val langFileParser: Parser[List[Language]] = fileParser

  val langTypes: List[Language] = types

  val languageParser: Parser[Language] =
    typeParser("language")(l => s"lang.${l.abbrev}")
}
