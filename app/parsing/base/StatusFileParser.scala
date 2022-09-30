package parsing.base

import basedata.Status
import javax.inject.Singleton

@Singleton
final class StatusFileParser extends LabelFileParser[Status] {
  override protected def makeType = Status.tupled
}
