package printing.latex.snippet

import play.api.i18n.Lang

final class LayoutContentSnippet extends LatexContentSnippet {
  override def print(using lang: Lang, builder: StringBuilder): Unit = {
    builder.append("\\layout*")
    finish
  }
}
