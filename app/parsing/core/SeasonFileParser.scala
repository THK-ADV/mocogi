package parsing.core

import models.core.Season

object SeasonFileParser extends LabelFileParser[Season] {
  override protected def makeType = (Season.apply _).tupled
}
