package parsing.base

import basedata.ModuleType
import javax.inject.Singleton

@Singleton
final class ModuleTypeFileParser extends LabelFileParser[ModuleType] {
  override protected def makeType = ModuleType.tupled
}
