package parsing.metadata

import basedata.Competence
import parser.Parser
import parsing.multipleValueParser

object CompetencesParser {

  def competencesParser(implicit
      competences: Seq[Competence]
  ): Parser[List[Competence]] =
    multipleValueParser(
      "competences",
      (c: Competence) => s"competence.${c.abbrev}",
      0
    )(competences.sortBy(_.abbrev).reverse)
}
