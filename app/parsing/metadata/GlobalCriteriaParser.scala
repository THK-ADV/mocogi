package parsing.metadata

import parser.Parser
import parser.Parser.{literal, oneOf}
import parsing.helper.MultipleValueParser
import parsing.types.GlobalCriteria

object GlobalCriteriaParser extends MultipleValueParser[GlobalCriteria] {
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
