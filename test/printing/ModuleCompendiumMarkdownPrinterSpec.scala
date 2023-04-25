package printing

import models.core._
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.types._
import parsing.withFile0
import printing.markdown.ModuleCompendiumMarkdownPrinter.printer
import validator.{
  Metadata,
  Module,
  ModuleRelation,
  POs,
  Prerequisites,
  Workload
}

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

final class ModuleCompendiumMarkdownPrinterSpec
    extends AnyWordSpec
    with EitherValues {

  "A ModuleCompendiumMarkdownPrinter" should {
    "print markdown file" in {
      val metadata = Metadata(
        UUID.randomUUID(),
        "title",
        "abbrev",
        ModuleType("module", "module", "module"),
        Some(ModuleRelation.Child(Module(UUID.randomUUID(), "abbrev"))),
        ECTS(5, Nil),
        Language("lang", "lang", "lang"),
        1,
        Season("season", "season", "season"),
        Responsibilities(
          List(Person.Unknown("unknown", "unknown")),
          List(Person.Unknown("unknown", "unknown"))
        ),
        AssessmentMethods(
          List(
            AssessmentMethodEntry(
              AssessmentMethod("method", "method", "method"),
              None,
              Nil
            )
          ),
          Nil
        ),
        Workload(0, 0, 0, 0, 0, 0, 0, 0),
        Prerequisites(None, None),
        Status("status", "status", "status"),
        Location("location", "location", "location"),
        POs(
          List(
            POMandatory(
              PO("po1", 0, LocalDate.now, LocalDate.now, None, Nil, "program1"),
              None,
              List(1),
              Nil
            ),
            POMandatory(
              PO("po2", 0, LocalDate.now, LocalDate.now, None, Nil, "program1"),
              None,
              Nil,
              Nil
            )
          ),
          Nil
        ),
        None,
        Nil,
        Nil,
        Nil
      )
      val mc = ModuleCompendium(
        metadata,
        Content(
          "* (Was) Die Studierenden sollen unterschiedliche Programmierparadigmen verstehen und anwenden können. Weiterhin sollen sie die Angemessenheit der verschiedenen Programmierparadigmen für eine Aufgabenstellung einordnen und bewerten können.\n* (Womit) Dies geschieht, indem verschieden Programmiersprachen eingeführt und praktisch ausprobiert werden. Dabei wird vergleichend auf die verschiedenen Ansätze (prozedural-funktional-objektorientiert; deklarativ-imperativ; textuell-visuell; Nebenläufigkeit; idiomatische Strukturen und Muster) eingegangen, indem Algorithmen in den verschiedenen Programmiersprachen umgesetzt werden.\n* (Wozu) Studierende sollen mithilfe von etablierten Paradigmen und Entwurfsmustern in der Lage sein, synchrone und asynchrone Programme zu konzipieren und ablaufsicher zu gestalten",
          "* Grundlagen von Programmiersprachen\n* Vergleich imperativer und deklarativer Paradigmen\n* Objektorientierte Design Prinzipien\n* prozedurale und objektorientierte Programmierung mit Kotlin\n* funktionale Programmierung mit Kotlin\n* Logikprogrammierung mit Prolog\n* Nebenläufigkeit (Thread-Sicherheit, Synchronisation, Deadlocks, Threads, Koroutinen)\n* Entwurfsmuster (u.a. Kompositum, Beobachter, Strategie, Dekorierer, Iterator, simple Factory)\n* Spaß haben\n* Programmieren",
          "* Vorlesung mit interaktiven Phasen, Präsentationen und Live-Coding\n* Übung\n* Praktikum\n* Selbststudium mit bereitgestellten Screencasts, einem umfassenden Skript sowie Fachliteratur\n\n4 SWS: 2 SWS Vorlesung + 1 SWS Übung + 1 SWS Praktikum",
          "* Skript (www.gm.fh-koeln.de/ehses/paradigmen/)\n* W.F. Clocksin, C.S. Mellish (2003). Programming in Prolog. Springer-Verlag\n* Tate, B. A., & Klicman, P. (2011). Sieben Wochen, sieben Sprachen: Verstehen Sie die modernen Sprachkonzepte. O’Reilly.\n* Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (2015). Design patterns: Entwurfsmuster als Elemente wiederverwendbarer\n* objektorientierter Software. Mitp.\n* Dmitry Jemerov & Svetlana Isakova (2017). Kotlin in Action. Manning Publications.\n* Pierre-Yves Saumont (2019). The Joy of Kotlin. Manning Publications.\n* Dawn Griffiths, David Griffiths & Jørgen W. Lang (2019). Kotlin von Kopf bis Fuß: Eine Einführung in die Kotlin-Programmierung. O'Reilly.\n* Thomas Theis (2019). Einstieg in Kotlin: Apps entwickeln mit Android Studio. Keine Vorkenntnisse erforderlich, ideal für Kotlin- Einsteiger und Java-Umsteiger. Rheinwerk-Verlag\n* Karl Szwillus (2019). Kotlin: Einstieg und Praxis. mitp Professional.\n* Kohls, C., Dobrynin, A., Leonhard, F. ( 2020). Programmieren lernen mit Kotlin. München: Hanser Verlag.\n* Online-Referenz und Tutorials: https://kotlinlang.org/docs/reference/",
          ""
        ),
        Content("", "", "", "", "")
      )
      val dePrinter =
        printer(_ => None)(PrintingLanguage.German, LocalDateTime.now())
      val deFile = withFile0("test/printing/res/de-print.md")(identity)
      println(dePrinter.print(mc, "").value)
      assert(
        dePrinter.print(mc, "").value.dropRight(10) == deFile.dropRight(10)
      )

      val enPrinter =
        printer(_ => None)(PrintingLanguage.English, LocalDateTime.now())
      val enFile = withFile0("test/printing/res/en-print.md")(identity)
      assert(
        enPrinter.print(mc, "").value.dropRight(10) == enFile.dropRight(10)
      )
    }
  }
}
