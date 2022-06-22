package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.ModuleType

import javax.inject.Singleton

trait ModuleTypeParser {
  val fileParser: Parser[List[ModuleType]]
  val parser: Parser[ModuleType]
}

@Singleton
final class ModuleTypeParserImpl(val path: String)
    extends ModuleTypeParser
    with SimpleFileParser[ModuleType] {

  override val makeType = ModuleType.tupled
  override val typename = "module types"

  val fileParser: Parser[List[ModuleType]] = makeFileParser

  val moduleTypes: List[ModuleType] = parseTypes

  val parser: Parser[ModuleType] =
    makeTypeParser("module_type")(t => s"module_type.${t.abbrev}")
}
