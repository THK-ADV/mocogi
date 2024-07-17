package parsing.core

import models.core.ModuleCompetence

object CompetenceFileParser extends LabelDescFileParser[ModuleCompetence] {
  def parser() = this.fileParser()

  override protected def makeType = {
    case (id, deLabel, enLabel, deDesc, enDesc) =>
      ModuleCompetence(id, deLabel, deDesc, enLabel, enDesc)
  }
}
