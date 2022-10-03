package parsing.metadata

import basedata.Competence
import parser.Parser
import parser.Parser.{literal, oneOf}
import parsing.helper.MultipleValueParser.multipleParser

object CompetencesParser {

  def competencesParser(implicit
      competences: Seq[Competence]
  ): Parser[List[Competence]] =
    multipleParser(
      "competences",
      oneOf(
        competences.map(c => literal(s"competence.${c.abbrev}").map(_ => c)): _*
      )
    )
}
