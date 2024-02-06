package parsing.core

import models.core.ModuleCompetence

object CompetenceFileParser extends LabelDescFileParser[ModuleCompetence] {
  override protected def makeType = (ModuleCompetence.apply _).tupled
}
