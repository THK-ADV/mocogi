package parsing.core

import models.core.AssessmentMethod

object AssessmentMethodFileParser extends LabelFileParser[AssessmentMethod] {
  override protected def makeType = (AssessmentMethod.apply _).tupled
}
