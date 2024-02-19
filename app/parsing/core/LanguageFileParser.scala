package parsing.core

import models.core.ModuleLanguage

object LanguageFileParser extends LabelFileParser[ModuleLanguage] {
  override protected def makeType = (ModuleLanguage.apply _).tupled
}
