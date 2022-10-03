package parsing.metadata

import basedata.GlobalCriteria
import parser.Parser
import parsing.multipleValueParser

object GlobalCriteriaParser {
  def globalCriteriaParser(implicit
      globalCriteria: Seq[GlobalCriteria]
  ): Parser[List[GlobalCriteria]] =
    multipleValueParser(
      "global_criteria",
      c => s"global_criteria.${c.abbrev}",
      0
    )
}
