package parsing.core

import models.core.ModuleStatus

object StatusFileParser extends LabelFileParser[ModuleStatus] {
  def parser() = super.fileParser()

  protected override def makeType = {
    case (id, deLabel, enLabel) =>
      ModuleStatus(id, deLabel, enLabel)
  }
}
