package parsing.base

import basedata.AssessmentMethod
import javax.inject.Singleton

@Singleton
final class AssessmentMethodFileParser
    extends LabelFileParser[AssessmentMethod] {
  override protected def makeType = AssessmentMethod.tupled
}
