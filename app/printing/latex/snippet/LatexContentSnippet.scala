package printing.latex.snippet

import play.api.i18n.Lang
import printing.latex.ModuleCatalogLatexPrinter

trait LatexContentSnippet {
  def print(using lang: Lang, builder: StringBuilder): Unit

  protected final def finish(using builder: StringBuilder): Unit = ModuleCatalogLatexPrinter.newPage()
}
