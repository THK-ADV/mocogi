package parsing.metadata.file

import basedata.ModuleType
import javax.inject.Singleton

@Singleton
final class ModuleTypeFileParser extends LabelFileParser[ModuleType] {
  override protected def makeType = ModuleType.tupled
}
