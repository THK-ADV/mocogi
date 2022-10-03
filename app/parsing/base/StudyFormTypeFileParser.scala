package parsing.base

import basedata.StudyFormType

object StudyFormTypeFileParser extends LabelFileParser[StudyFormType] {
  override protected def makeType = StudyFormType.tupled
}
