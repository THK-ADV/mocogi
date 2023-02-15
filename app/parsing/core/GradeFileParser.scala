package parsing.core

import models.core.Grade

object GradeFileParser extends LabelDescFileParser[Grade] {
  override protected def makeType = Grade.tupled
}
