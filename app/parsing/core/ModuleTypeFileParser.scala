package parsing.core

import models.core.ModuleType
import javax.inject.Singleton

@Singleton
final class ModuleTypeFileParser extends LabelFileParser[ModuleType] {
  override protected def makeType = ModuleType.tupled
}
