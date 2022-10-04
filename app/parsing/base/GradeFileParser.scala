package parsing.base

import basedata.Grade

object GradeFileParser extends LabelDescFileParser[Grade] {
  override protected def makeType = Grade.tupled
}
