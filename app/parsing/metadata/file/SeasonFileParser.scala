package parsing.metadata.file

import parsing.helper.SimpleFileParser2
import parsing.types.Season

import javax.inject.Singleton

@Singleton
final class SeasonFileParser extends SimpleFileParser2[Season] {
  override protected def makeType = Season.tupled
}
