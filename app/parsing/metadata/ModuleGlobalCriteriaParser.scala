package parsing.metadata

import models.core.ModuleGlobalCriteria
import parser.Parser
import parsing.{multipleValueParser, multipleValueRawParser}

object ModuleGlobalCriteriaParser {
  def key = "global_criteria"
  def prefix = "global_criteria."

  def parser(implicit
      globalCriteria: Seq[ModuleGlobalCriteria]
  ): Parser[List[ModuleGlobalCriteria]] =
    multipleValueParser(
      key,
      (g: ModuleGlobalCriteria) => s"$prefix${g.id}"
    )(globalCriteria.sortBy(_.id).reverse)

  def raw: Parser[List[String]] =
    multipleValueRawParser(key, prefix)
}
