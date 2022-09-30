package parsing.metadata.file

import basedata.GlobalCriteria
import javax.inject.Singleton

@Singleton
final class GlobalCriteriaFileParser extends LabelDescFileParser[GlobalCriteria] {
  override protected def makeType = GlobalCriteria.tupled
}
