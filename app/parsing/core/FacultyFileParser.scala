package parsing.core

import models.core.Faculty

object FacultyFileParser extends LabelFileParser[Faculty] {
  def parser() = super.fileParser()

  override protected def makeType = { case (id, deLabel, enLabel) =>
    Faculty(id, deLabel, enLabel)
  }
}
