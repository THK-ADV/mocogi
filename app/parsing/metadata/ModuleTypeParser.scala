package parsing.metadata

import parser.Parser
import parsing.helper.SimpleFileParser3
import parsing.types.ModuleType

import javax.inject.Singleton

@Singleton
final class ModuleTypeParser extends SimpleFileParser3[ModuleType] {
  def parser(implicit moduleTypes: Seq[ModuleType]): Parser[ModuleType] =
    makeTypeParser("module_type", moduleTypes, x => s"module_type.${x.abbrev}")
}
