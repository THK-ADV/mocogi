package parsing.base

import basedata.Faculty

object FacultyFileParser extends LabelFileParser[Faculty] {
  override protected def makeType = Faculty.tupled
}
