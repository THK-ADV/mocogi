package parsing.metadata.file

import basedata.Season
import javax.inject.Singleton

@Singleton
final class SeasonFileParser extends LabelFileParser[Season] {
  override protected def makeType = Season.tupled
}
