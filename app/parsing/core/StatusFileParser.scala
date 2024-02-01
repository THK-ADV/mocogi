package parsing.core

import models.core.Status

object StatusFileParser extends LabelFileParser[Status] {
  override protected def makeType = (Status.apply _).tupled
}
