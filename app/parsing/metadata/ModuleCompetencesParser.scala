package parsing.metadata

import models.core.ModuleCompetence
import parser.Parser
import parsing.multipleValueRawParser

@Deprecated
object ModuleCompetencesParser {
  def key    = "competences"
  def prefix = "competence."

  def parser: Parser[List[ModuleCompetence]] =
    raw.map(_.map(ModuleCompetence(_, "", "", "", "")))

  def raw: Parser[List[String]] =
    multipleValueRawParser(key, prefix)
}
