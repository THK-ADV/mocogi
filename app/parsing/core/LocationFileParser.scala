package parsing.core

import models.core.ModuleLocation

object LocationFileParser extends LabelFileParser[ModuleLocation] {
  def parser() = super.fileParser()

  override protected def makeType = { case (id, deLabel, enLabel) =>
    ModuleLocation(id, deLabel, enLabel)
  }
}
