package parsing.core

import models.core.Degree

object DegreeFileParser extends LabelDescFileParser[Degree] {
  override protected def makeType = (Degree.apply _).tupled
}
