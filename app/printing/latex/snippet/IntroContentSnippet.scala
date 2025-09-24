package printing.latex.snippet

import java.nio.file.Path

import play.api.i18n.Lang

final class IntroContentSnippet(textFileName: Path) extends LatexContentSnippet {
  override def print(using lang: Lang, builder: StringBuilder): Unit = {
    builder.append(s"""\\chapter{Prolog}
                      |\\newpage
                      |\\include{$textFileName}""".stripMargin)
    finish
  }
}
