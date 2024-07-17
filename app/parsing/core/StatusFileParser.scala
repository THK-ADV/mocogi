package parsing.core

import models.core.ModuleStatus

object StatusFileParser extends LabelFileParser[ModuleStatus] {
  def parser() = super.fileParser()

  override protected def makeType = { case (id, deLabel, enLabel) =>
    ModuleStatus(id, deLabel, enLabel)
  }
}
