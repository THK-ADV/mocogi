package parsing.metadata

import parser.Parser
import parser.Parser.{literal, oneOf}
import parsing.helper.MultipleValueParser.multipleParser
import parsing.types.Competence

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
