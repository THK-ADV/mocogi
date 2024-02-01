package parsing.core

import models.core.Competence

object CompetenceFileParser extends LabelDescFileParser[Competence] {
  override protected def makeType = (Competence.apply _).tupled
}
