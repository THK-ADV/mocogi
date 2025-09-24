package printing

import org.scalatest.wordspec.AnyWordSpec
import parsing.types.ModuleContent
import play.api.i18n.DefaultMessagesApi
import printing.yaml.ContentMarkdownPrinter

// https://www.playframework.com/documentation/3.0.x/ScalaTestingWithSpecs2#Unit-Testing-Messages
final class ModuleContentMarkdownPrinterSpec extends AnyWordSpec with PrinterSpec {

  val messagesApi =
    new DefaultMessagesApi(
      Map(
        "de" -> Map(
          "latex.module_catalog.content.learning_outcome" -> "Angestrebte Lernergebnisse",
          "latex.module_catalog.content.module"           -> "Modulinhalte",
          "latex.module_catalog.content.teaching_methods" -> "Lehr- und Lernmethoden (Medienformen)",
          "latex.module_catalog.content.reading"          -> "Empfohlene Literatur",
          "latex.module_catalog.content.particularities"  -> "Besonderheiten",
        ),
        "en" -> Map(
          "latex.module_catalog.content.learning_outcome" -> "Learning Outcome",
          "latex.module_catalog.content.module"           -> "Module Content",
          "latex.module_catalog.content.teaching_methods" -> "Teaching and Learning Methods",
          "latex.module_catalog.content.reading"          -> "Recommended Reading",
          "latex.module_catalog.content.particularities"  -> "Particularities",
        ),
      )
    )

  val printer = new ContentMarkdownPrinter(messagesApi)

  "A Content Printer" should {
    "print" in {
      val de = ModuleContent(
        "Programmieren lernen",
        "- Klassen\n- Vererbung\n- Polymorphie",
        "Slides, Whiteboard",
        "Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt",
        "nichts"
      )
      val en = ModuleContent(
        "Learn to code",
        "- Classes\n- Inheritance\n- Polymorphism",
        "",
        "Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt",
        "nothing"
      )
      val res =
        s"""## (de) Angestrebte Lernergebnisse:
           |
           |Programmieren lernen
           |
           |## (en) Learning Outcome:
           |
           |Learn to code
           |
           |## (de) Modulinhalte:
           |
           |- Klassen
           |- Vererbung
           |- Polymorphie
           |
           |## (en) Module Content:
           |
           |- Classes
           |- Inheritance
           |- Polymorphism
           |
           |## (de) Lehr- und Lernmethoden (Medienformen):
           |
           |Slides, Whiteboard
           |
           |## (en) Teaching and Learning Methods:
           |
           |## (de) Empfohlene Literatur:
           |
           |Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt
           |
           |## (en) Recommended Reading:
           |
           |Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt
           |
           |## (de) Besonderheiten:
           |
           |nichts
           |
           |## (en) Particularities:
           |
           |nothing\n""".stripMargin
      assert(
        printer
          .printer()
          .print((de, en), new StringBuilder())
          .value
          .toString() == res
      )
    }
  }
}
