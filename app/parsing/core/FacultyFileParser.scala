package parsing.core

import models.core.Faculty

object FacultyFileParser extends LabelFileParser[Faculty] {
  override protected def makeType = (Faculty.apply _).tupled
}
