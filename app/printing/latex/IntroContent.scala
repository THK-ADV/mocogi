package printing.latex

import play.api.i18n.Lang
import printing.PrintingLanguage

trait IntroContent {
  def printWithNewPage(
      pLang: PrintingLanguage,
      lang: Lang,
      builder: StringBuilder
  ): Unit
}
