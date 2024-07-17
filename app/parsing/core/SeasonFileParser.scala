package parsing.core

import models.core.Season

object SeasonFileParser extends LabelFileParser[Season] {
  def parser() = super.fileParser()

  override protected def makeType = { case (id, deLabel, enLabel) =>
    Season(id, deLabel, enLabel)
  }
}
