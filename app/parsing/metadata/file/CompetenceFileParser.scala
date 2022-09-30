package parsing.metadata.file

import basedata.Competence
import javax.inject.Singleton

@Singleton
class CompetenceFileParser extends LabelDescFileParser[Competence] {
  override protected def makeType = Competence.tupled
}
