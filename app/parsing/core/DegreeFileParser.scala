package parsing.core

import models.core.Degree

object DegreeFileParser extends LabelDescFileParser[Degree] {
  def parser() = super.fileParser()

  protected override def makeType = {
    case (id, deLabel, enLabel, deDesc, enDesc) =>
      Degree(id, deLabel, deDesc, enLabel, enDesc)
  }
}
