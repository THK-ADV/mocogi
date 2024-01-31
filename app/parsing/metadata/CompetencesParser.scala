package parsing.metadata

import models.core.Competence
import parser.Parser
import parsing.multipleValueParser

object CompetencesParser {

  def competencesParser(implicit
      competences: Seq[Competence]
  ): Parser[List[Competence]] =
    multipleValueParser(
      "competences",
      (c: Competence) => s"competence.${c.id}",
      0
    )(competences.sortBy(_.id).reverse)
}
