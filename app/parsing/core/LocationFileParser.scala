package parsing.core

import models.core.ModuleLocation

object LocationFileParser extends LabelFileParser[ModuleLocation] {
  def parser() = super.fileParser()

  protected override def makeType = {
    case (id, deLabel, enLabel) =>
      ModuleLocation(id, deLabel, enLabel)
  }
}
