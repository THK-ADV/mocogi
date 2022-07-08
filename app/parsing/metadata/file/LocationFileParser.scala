package parsing.metadata.file

import parsing.types.Location

import javax.inject.Singleton

@Singleton
final class LocationFileParser extends SimpleFileParser[Location] {
  override protected def makeType = Location.tupled
}
