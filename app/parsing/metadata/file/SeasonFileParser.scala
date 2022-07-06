package parsing.metadata.file

import parsing.helper.SimpleFileParser
import parsing.types.Season

import javax.inject.Singleton

@Singleton
final class SeasonFileParser extends SimpleFileParser[Season] {
  override protected def makeType = Season.tupled
}
