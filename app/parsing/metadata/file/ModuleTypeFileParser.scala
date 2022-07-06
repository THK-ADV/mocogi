package parsing.metadata.file

import parsing.helper.SimpleFileParser2
import parsing.types.ModuleType

import javax.inject.Singleton

@Singleton
final class ModuleTypeFileParser extends SimpleFileParser2[ModuleType] {
  override protected def makeType = ModuleType.tupled
}
