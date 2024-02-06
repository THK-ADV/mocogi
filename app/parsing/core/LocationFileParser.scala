package parsing.core

import models.core.ModuleLocation

object LocationFileParser extends LabelFileParser[ModuleLocation] {
  override protected def makeType = (ModuleLocation.apply _).tupled
}
