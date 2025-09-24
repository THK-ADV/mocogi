package printing.latex.snippet

import cats.data.NonEmptyList
import models.ModuleCore
import models.ModuleKey
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import printing.latex.ModuleCatalogLatexPrinter

final class DiffContentSnippet(diffs: NonEmptyList[(ModuleCore, Set[String])], messagesApi: MessagesApi)
    extends LatexContentSnippet {

  import ModuleCatalogLatexPrinter.chapter
  import ModuleCatalogLatexPrinter.nameRef
  import ModuleCatalogLatexPrinter.newPage

  override def print(using lang: Lang, builder: StringBuilder): Unit = {
    val sectionTitle = messagesApi("latex.module_catalog.module_diff.section.title")
    val sectionIntro = messagesApi("latex.module_catalog.module_diff.section.intro")
    chapter(sectionTitle)
    newPage()
    builder.append(s"$sectionIntro\n")
    diffs
      .filter(_._2.nonEmpty)
      .sortBy(_._1.title)
      .foreach {
        case (module, changedKeys) =>
          val ref = nameRef(module.id)
          builder.append(s"\\subsection*{$ref}\n")
          builder.append("\\begin{itemize}\n")
          changedKeys.toList.sorted.foreach { key =>
            val normalizedKey = ModuleKey.normalizeKeyValue(key)
            val label         = messagesApi(normalizedKey + ".label")
            builder.append(s"\\item $label\n")
          }
          builder.append("\\end{itemize}\n")
      }
    finish
  }
}
