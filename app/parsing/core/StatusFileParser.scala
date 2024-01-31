package parsing.core

import models.core.Status
import javax.inject.Singleton

@Singleton
final class StatusFileParser extends LabelFileParser[Status] {
  override protected def makeType = (Status.apply _).tupled
}
