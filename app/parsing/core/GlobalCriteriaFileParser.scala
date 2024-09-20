package parsing.core

import models.core.ModuleGlobalCriteria

object GlobalCriteriaFileParser extends LabelDescFileParser[ModuleGlobalCriteria] {
  def parser() = super.fileParser()

  protected override def makeType = {
    case (id, deLabel, enLabel, deDesc, enDesc) =>
      ModuleGlobalCriteria(id, deLabel, deDesc, enLabel, enDesc)
  }
}
