package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser
import parsing.types.ModuleType

object ModuleTypeParser extends SimpleFileParser[ModuleType] {

  override val makeType = ModuleType.tupled
  override val filename = "module_type.yaml"
  override val typename = "module types"

  val moduleTypesFileParser: Parser[List[ModuleType]] = fileParser

  val moduleTypes: List[ModuleType] = types

  val moduleTypeParser: Parser[ModuleType] =
    typeParser("module_type")(t => s"module_type.${t.abbrev}")
}
