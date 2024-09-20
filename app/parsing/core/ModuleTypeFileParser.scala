package parsing.core

import models.core.ModuleType

object ModuleTypeFileParser extends LabelFileParser[ModuleType] {
  def parser() = super.fileParser()

  protected override def makeType = {
    case (id, deLabel, enLabel) =>
      ModuleType(id, deLabel, enLabel)
  }
}
