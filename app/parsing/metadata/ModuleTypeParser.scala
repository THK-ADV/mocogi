package parsing.metadata

import parser.Parser
import parsing.helper.SingleValueParser
import parsing.types.ModuleType

import javax.inject.Singleton

@Singleton
final class ModuleTypeParser extends SingleValueParser[ModuleType] {
  def parser(implicit moduleTypes: Seq[ModuleType]): Parser[ModuleType] =
    itemParser("module_type", moduleTypes, x => s"module_type.${x.abbrev}")
}
