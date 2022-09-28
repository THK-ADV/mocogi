package parsing.metadata.file

import parsing.types.Language

import javax.inject.Singleton

@Singleton
final class LanguageFileParser extends LabelFileParser[Language] {
  override protected def makeType = Language.tupled
}
