package printing.latex.snippet

import play.api.i18n.Lang
import printing.latex.escape
import service.artifact.modulecatalog.ModuleCatalogWarning

final class DiagnosticsContentSnippet(warnings: List[ModuleCatalogWarning]) extends LatexContentSnippet {
  override def print(using lang: Lang, builder: StringBuilder): Unit = {
    if warnings.nonEmpty then {
      builder.append("\\chapter*{Hinweise zur Vorschau}\n")
      builder.append("\\begin{itemize}\n")
      warnings.foreach { warning =>
        val module = warning.moduleId.fold("")(id => s" (${id.toString})")
        builder.append(s"\\item \\textbf{${escape(warning.code)}}: ${escape(warning.message + module)}\n")
      }
      builder.append("\\end{itemize}\n")
    }
  }
}
