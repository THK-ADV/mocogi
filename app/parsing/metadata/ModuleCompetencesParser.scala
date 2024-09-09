package parsing.metadata

import models.core.ModuleCompetence
import parser.Parser
import parsing.{multipleValueParser, multipleValueRawParser}

object ModuleCompetencesParser {
  def key = "competences"
  def prefix = "competence."

  def parser(implicit
      competences: Seq[ModuleCompetence]
  ): Parser[List[ModuleCompetence]] =
    multipleValueParser(
      key,
      (c: ModuleCompetence) => s"$prefix${c.id}"
    )(competences.sortBy(_.id).reverse)

  def raw: Parser[List[String]] =
    multipleValueRawParser(key, prefix)
}
