package parsing.core

import models.core.Location

object LocationFileParser extends LabelFileParser[Location] {
  override protected def makeType = (Location.apply _).tupled
}
