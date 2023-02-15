package parsing.core

import models.core.Season
import javax.inject.Singleton

@Singleton
final class SeasonFileParser extends LabelFileParser[Season] {
  override protected def makeType = Season.tupled
}
