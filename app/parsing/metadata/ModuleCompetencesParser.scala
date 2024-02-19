package parsing.metadata

import models.core.ModuleCompetence
import parser.Parser
import parsing.multipleValueParser

object ModuleCompetencesParser {

  def competencesParser(implicit
      competences: Seq[ModuleCompetence]
  ): Parser[List[ModuleCompetence]] =
    multipleValueParser(
      "competences",
      (c: ModuleCompetence) => s"competence.${c.id}",
      0
    )(competences.sortBy(_.id).reverse)
}
