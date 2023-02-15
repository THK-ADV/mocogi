package parsing.core

import models.core.Language
import javax.inject.Singleton

@Singleton
final class LanguageFileParser extends LabelFileParser[Language] {
  override protected def makeType = Language.tupled
}
