package parsing.base

import basedata.AssessmentMethod

object AssessmentMethodFileParser extends LabelFileParser[AssessmentMethod] {
  override protected def makeType = AssessmentMethod.tupled
}
