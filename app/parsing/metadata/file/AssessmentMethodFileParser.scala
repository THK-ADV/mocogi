package parsing.metadata.file

import parsing.types.AssessmentMethod

import javax.inject.Singleton

@Singleton
final class AssessmentMethodFileParser
    extends SimpleFileParser[AssessmentMethod] {
  override protected def makeType = AssessmentMethod.tupled
}
