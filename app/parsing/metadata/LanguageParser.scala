package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.Language

object LanguageParser extends SimpleFileParser[Language] {

  override val makeType = Language.tupled

  override val path = "res/lang.yaml"

  override val typename = "languages"

  val langFileParser: Parser[List[Language]] = makeFileParser

  val langTypes: List[Language] = parseTypes

  val languageParser: Parser[Language] =
    makeTypeParser("language")(l => s"lang.${l.abbrev}")
}
