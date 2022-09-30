package parsing.metadata

import basedata.GlobalCriteria
import parser.Parser
import parser.Parser.{literal, oneOf}
import parsing.helper.MultipleValueParser.multipleParser

object GlobalCriteriaParser {
  def globalCriteriaParser(implicit
      globalCriteria: Seq[GlobalCriteria]
  ): Parser[List[GlobalCriteria]] =
    multipleParser(
      "global_criteria",
      oneOf(
        globalCriteria.map(c =>
          literal(s"global_criteria.${c.abbrev}").map(_ => c)
        ): _*
      )
    )
}
