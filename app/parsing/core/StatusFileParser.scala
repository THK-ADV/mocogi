package parsing.core

import models.core.ModuleStatus

object StatusFileParser extends LabelFileParser[ModuleStatus] {
  override protected def makeType = (ModuleStatus.apply _).tupled
}
