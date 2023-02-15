package parsing.core

import models.core.Competence
import javax.inject.Singleton

@Singleton
class CompetenceFileParser extends LabelDescFileParser[Competence] {
  override protected def makeType = Competence.tupled
}
