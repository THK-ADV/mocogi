package parsing.core

import models.core.Language

object LanguageFileParser extends LabelFileParser[Language] {
  override protected def makeType = (Language.apply _).tupled
}
