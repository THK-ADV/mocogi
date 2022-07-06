package parsing.metadata.file

import parsing.types.ModuleType

import javax.inject.Singleton

@Singleton
final class ModuleTypeFileParser extends SimpleFileParser[ModuleType] {
  override protected def makeType = ModuleType.tupled
}
