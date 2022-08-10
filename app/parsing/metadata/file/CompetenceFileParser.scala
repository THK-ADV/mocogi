package parsing.metadata.file

import parsing.types.Competence

import javax.inject.Singleton

@Singleton
class CompetenceFileParser extends LabelDescFileParser[Competence] {
  override protected def makeType = Competence.tupled
}
