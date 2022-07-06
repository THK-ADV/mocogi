package parsing.metadata.file

import parsing.helper.SimpleFileParser2
import parsing.types.Language

import javax.inject.Singleton

@Singleton
final class LanguageFileParser extends SimpleFileParser2[Language] {
  override protected def makeType = Language.tupled
}
