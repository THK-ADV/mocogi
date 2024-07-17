package parsing.modulekeys

import models.ModuleKey
import parsing.core.LabelDescFileParser

import javax.inject.Singleton

@Singleton
final class ModuleKeyParser extends LabelDescFileParser[ModuleKey] {
  def parser() = super.fileParser()

  override protected def makeType = {
    case (id, deLabel, enLabel, deDesc, enDesc) =>
      ModuleKey(id, deLabel, deDesc, enLabel, enDesc)
  }
}
