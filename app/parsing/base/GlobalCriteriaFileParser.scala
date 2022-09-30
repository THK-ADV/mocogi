package parsing.base

import basedata.GlobalCriteria
import javax.inject.Singleton

@Singleton
final class GlobalCriteriaFileParser
    extends LabelDescFileParser[GlobalCriteria] {
  override protected def makeType = GlobalCriteria.tupled
}
