package parsing.metadata

import models.core.GlobalCriteria
import parser.Parser
import parsing.multipleValueParser

object GlobalCriteriaParser {
  def globalCriteriaParser(implicit
      globalCriteria: Seq[GlobalCriteria]
  ): Parser[List[GlobalCriteria]] =
    multipleValueParser(
      "global_criteria",
      (g: GlobalCriteria) => s"global_criteria.${g.id}",
      0
    )(globalCriteria.sortBy(_.id).reverse)
}
