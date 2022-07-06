package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileItemParser
import parsing.types.ModuleType

import javax.inject.Singleton

@Singleton
final class ModuleTypeParser extends SimpleFileItemParser[ModuleType] {
  def parser(implicit moduleTypes: Seq[ModuleType]): Parser[ModuleType] =
    itemParser("module_type", moduleTypes, x => s"module_type.${x.abbrev}")
}
