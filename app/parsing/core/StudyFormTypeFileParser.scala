package parsing.core

import models.core.StudyFormType

object StudyFormTypeFileParser extends LabelFileParser[StudyFormType] {
  override protected def makeType = StudyFormType.tupled
}
