package parsing.metadata

import models.core.ModuleGlobalCriteria
import parser.Parser
import parsing.multipleValueRawParser

@Deprecated
object ModuleGlobalCriteriaParser {
  def key    = "global_criteria"
  def prefix = "global_criteria."

  def parser: Parser[List[ModuleGlobalCriteria]] =
    raw.map(_.map(ModuleGlobalCriteria(_, "", "", "", "")))

  def raw: Parser[List[String]] =
    multipleValueRawParser(key, prefix)
}
