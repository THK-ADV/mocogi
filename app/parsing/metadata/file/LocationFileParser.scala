package parsing.metadata.file

import parsing.types.Location

import javax.inject.Singleton

@Singleton
final class LocationFileParser extends LabelFileParser[Location] {
  override protected def makeType = Location.tupled
}
