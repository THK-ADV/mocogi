package parsing.metadata

import models.core.ModuleGlobalCriteria
import parser.Parser
import parsing.multipleValueParser

object ModuleGlobalCriteriaParser {
  def globalCriteriaParser(implicit
      globalCriteria: Seq[ModuleGlobalCriteria]
  ): Parser[List[ModuleGlobalCriteria]] =
    multipleValueParser(
      "global_criteria",
      (g: ModuleGlobalCriteria) => s"global_criteria.${g.id}",
      0
    )(globalCriteria.sortBy(_.id).reverse)
}
