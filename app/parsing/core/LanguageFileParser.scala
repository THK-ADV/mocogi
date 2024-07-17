package parsing.core

import models.core.ModuleLanguage

object LanguageFileParser extends LabelFileParser[ModuleLanguage] {
  def parser() = super.fileParser()

  override protected def makeType = { case (id, deLabel, enLabel) =>
    ModuleLanguage(id, deLabel, enLabel)
  }
}
