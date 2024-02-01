package parsing.core

import models.core.ModuleType

object ModuleTypeFileParser extends LabelFileParser[ModuleType] {
  override protected def makeType = (ModuleType.apply _).tupled
}
