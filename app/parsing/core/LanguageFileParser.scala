package parsing.core

import models.core.ModuleLanguage

object LanguageFileParser extends LabelFileParser[ModuleLanguage] {
  def parser() = super.fileParser()

  protected override def makeType = {
    case (id, deLabel, enLabel) =>
      ModuleLanguage(id, deLabel, enLabel)
  }
}
