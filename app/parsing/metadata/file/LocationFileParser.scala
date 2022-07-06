package parsing.metadata.file

import parsing.helper.SimpleFileParser2
import parsing.types.Location

import javax.inject.Singleton

@Singleton
final class LocationFileParser extends SimpleFileParser2[Location] {
  override protected def makeType = Location.tupled
}
