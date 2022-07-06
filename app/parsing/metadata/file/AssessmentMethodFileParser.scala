package parsing.metadata.file

import parsing.helper.SimpleFileParser2
import parsing.types.AssessmentMethod

import javax.inject.Singleton

@Singleton
final class AssessmentMethodFileParser
    extends SimpleFileParser2[AssessmentMethod] {
  override protected def makeType = AssessmentMethod.tupled
}
