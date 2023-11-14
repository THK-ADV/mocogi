package parsing.modulekeys

import models.ModuleKey
import parsing.core.LabelDescFileParser

import javax.inject.Singleton

@Singleton
final class ModuleKeyParser extends LabelDescFileParser[ModuleKey] {
  override protected def makeType = (ModuleKey.apply _).tupled
}
