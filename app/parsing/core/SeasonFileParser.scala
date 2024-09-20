package parsing.core

import models.core.Season

object SeasonFileParser extends LabelFileParser[Season] {
  def parser() = super.fileParser()

  protected override def makeType = {
    case (id, deLabel, enLabel) =>
      Season(id, deLabel, enLabel)
  }
}
