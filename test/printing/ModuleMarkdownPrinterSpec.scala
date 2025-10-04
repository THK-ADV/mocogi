package printing

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

import cats.data.NonEmptyList
import models.*
import models.core.*
import models.core.ExamPhases.ExamPhase
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.types.*
import parsing.withFile0
import play.api.i18n.DefaultMessagesApi
import printing.markdown.ModuleMarkdownPrinter

final class ModuleMarkdownPrinterSpec extends AnyWordSpec with EitherValues {

  val messagesApi =
    new DefaultMessagesApi(
      Map(
        "de" -> Map(
          "latex.module_catalog.none"                        -> "Keine",
          "latex.module_catalog.unknown"                     -> "Keine Angabe",
          "latex.module_catalog.semester"                    -> "Semester",
          "latex.module_catalog.module_abbrev"               -> "Modulnummer",
          "latex.module_catalog.module_title"                -> "Modulbezeichnung",
          "latex.module_catalog.module_type"                 -> "Art des Moduls",
          "latex.module_catalog.ects"                        -> "ECTS credits",
          "latex.module_catalog.language"                    -> "Sprache",
          "latex.module_catalog.duration"                    -> "Dauer des Moduls",
          "latex.module_catalog.recommended_semester"        -> "Empfohlenes Studiensemester",
          "latex.module_catalog.frequency"                   -> "Häufigkeit des Angebots",
          "latex.module_catalog.module_management"           -> "Modulverantwortliche*r",
          "latex.module_catalog.lecturer"                    -> "Dozierende",
          "latex.module_catalog.assessment_method"           -> "Prüfungsformen",
          "latex.module_catalog.workload"                    -> "Workload",
          "latex.module_catalog.workload.contact_hours"      -> "Präsenzzeit",
          "latex.module_catalog.workload.self_study"         -> "Selbststudium",
          "latex.module_catalog.workload.lecture"            -> "{0} h Vorlesung",
          "latex.module_catalog.workload.exercise"           -> "{0} h Übung",
          "latex.module_catalog.workload.practical"          -> "{0} h Praktikum",
          "latex.module_catalog.workload.seminar"            -> "{0} h Seminar",
          "latex.module_catalog.workload.projectSupervision" -> "{0} h Projektbetreuung",
          "latex.module_catalog.workload.projectWork"        -> "{0} h Projektarbeit",
          "latex.module_catalog.prerequisite.recommended"    -> "Empfohlene Voraussetzungen",
          "latex.module_catalog.prerequisite.required"       -> "Zwingende Voraussetzungen",
          "latex.module_catalog.prerequisite.attendance"     -> "Anwesenheitspflicht",
          "latex.module_catalog.prerequisite.assessment"     -> "Prüfungsvorleistung",
          "latex.module_catalog.prerequisite.module"         -> "Module",
          "latex.module_catalog.po"                          -> "Verwendung des Moduls in weiteren Studiengängen",
          "latex.module_catalog.po_short"                    -> "In anderen Studiengängen",
          "latex.module_catalog.last_modified"               -> "Letzte Aktualisierung am",
          "latex.module_catalog.parent"                      -> "Besteht aus den Teilmodulen",
          "latex.module_catalog.child"                       -> "Gehört zum Modul",
          "latex.module_catalog.content.learning_outcome"    -> "Angestrebte Lernergebnisse",
          "latex.module_catalog.content.module"              -> "Modulinhalte",
          "latex.module_catalog.content.teaching_methods"    -> "Lehr- und Lernmethoden (Medienformen)",
          "latex.module_catalog.content.reading"             -> "Empfohlene Literatur",
          "latex.module_catalog.content.particularities"     -> "Besonderheiten",
          "latex.module_catalog.season"                      -> "Jedes {0}",
        )
      )
    )

  val printer = new ModuleMarkdownPrinter(messagesApi, false)

  "A Module Markdown Printer" should {
    "print markdown file" in {
      val metadata = Metadata(
        UUID.randomUUID(),
        "title",
        "abbrev",
        ModuleType("module", "module", "module"),
        Some(
          ModuleRelation.Child(ModuleCore(UUID.randomUUID(), "title", "abbrev"))
        ),
        ModuleECTS(5, Nil),
        ModuleLanguage("lang", "lang", "lang"),
        1,
        Season("season", "season", "season"),
        ModuleResponsibilities(
          NonEmptyList.one(Identity.Unknown("unknown", "unknown")),
          NonEmptyList.one(Identity.Unknown("unknown", "unknown"))
        ),
        ModuleAssessmentMethods(
          List(
            ModuleAssessmentMethodEntry(
              AssessmentMethod("method", "method", "method"),
              None,
              Nil
            )
          )
        ),
        Examiner(Identity.NN, Identity.NN),
        ExamPhase.all,
        ModuleWorkload(0, 0, 34, 11, 0, 0),
        ModulePrerequisites(None, None),
        ModuleStatus("status", "status", "status"),
        ModuleLocation("location", "location", "location"),
        ModulePOs(
          List(
            ModulePOMandatory(
              PO("po1", 0, "program1", LocalDate.now, None, 30),
              None,
              List(1)
            ),
            ModulePOMandatory(
              PO("po2", 0, "program1", LocalDate.now, None, 30),
              None,
              Nil
            )
          ),
          Nil
        ),
        None,
        Nil,
        Nil,
        Nil,
        None,
        None
      )
      val module = Module(
        metadata,
        ModuleContent(
          "* (Was) Die Studierenden sollen unterschiedliche Programmierparadigmen verstehen und anwenden können. Weiterhin sollen sie die Angemessenheit der verschiedenen Programmierparadigmen für eine Aufgabenstellung einordnen und bewerten können.\n* (Womit) Dies geschieht, indem verschieden Programmiersprachen eingeführt und praktisch ausprobiert werden. Dabei wird vergleichend auf die verschiedenen Ansätze (prozedural-funktional-objektorientiert; deklarativ-imperativ; textuell-visuell; Nebenläufigkeit; idiomatische Strukturen und Muster) eingegangen, indem Algorithmen in den verschiedenen Programmiersprachen umgesetzt werden.\n* (Wozu) Studierende sollen mithilfe von etablierten Paradigmen und Entwurfsmustern in der Lage sein, synchrone und asynchrone Programme zu konzipieren und ablaufsicher zu gestalten",
          "* Grundlagen von Programmiersprachen\n* Vergleich imperativer und deklarativer Paradigmen\n* Objektorientierte Design Prinzipien\n* prozedurale und objektorientierte Programmierung mit Kotlin\n* funktionale Programmierung mit Kotlin\n* Logikprogrammierung mit Prolog\n* Nebenläufigkeit (Thread-Sicherheit, Synchronisation, Deadlocks, Threads, Koroutinen)\n* Entwurfsmuster (u.a. Kompositum, Beobachter, Strategie, Dekorierer, Iterator, simple Factory)\n* Spaß haben\n* Programmieren",
          "* Vorlesung mit interaktiven Phasen, Präsentationen und Live-Coding\n* Übung\n* Praktikum\n* Selbststudium mit bereitgestellten Screencasts, einem umfassenden Skript sowie Fachliteratur\n\n4 SWS: 2 SWS Vorlesung + 1 SWS Übung + 1 SWS Praktikum",
          "* Skript (www.gm.fh-koeln.de/ehses/paradigmen/)\n* W.F. Clocksin, C.S. Mellish (2003). Programming in Prolog. Springer-Verlag\n* Tate, B. A., & Klicman, P. (2011). Sieben Wochen, sieben Sprachen: Verstehen Sie die modernen Sprachkonzepte. O’Reilly.\n* Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (2015). Design patterns: Entwurfsmuster als Elemente wiederverwendbarer\n* objektorientierter Software. Mitp.\n* Dmitry Jemerov & Svetlana Isakova (2017). Kotlin in Action. Manning Publications.\n* Pierre-Yves Saumont (2019). The Joy of Kotlin. Manning Publications.\n* Dawn Griffiths, David Griffiths & Jørgen W. Lang (2019). Kotlin von Kopf bis Fuß: Eine Einführung in die Kotlin-Programmierung. O'Reilly.\n* Thomas Theis (2019). Einstieg in Kotlin: Apps entwickeln mit Android Studio. Keine Vorkenntnisse erforderlich, ideal für Kotlin- Einsteiger und Java-Umsteiger. Rheinwerk-Verlag\n* Karl Szwillus (2019). Kotlin: Einstieg und Praxis. mitp Professional.\n* Kohls, C., Dobrynin, A., Leonhard, F. ( 2020). Programmieren lernen mit Kotlin. München: Hanser Verlag.\n* Online-Referenz und Tutorials: https://kotlinlang.org/docs/reference/",
          ""
        ),
        ModuleContent("", "", "", "", "")
      )
      val dePrinter =
        printer.printer(_ => None)(LocalDateTime.now())
      val deFile = withFile0("test/printing/res/de-print.md")(identity)
      assert(
        dePrinter
          .print(module, new StringBuilder())
          .value
          .toString()
          .dropRight(16) == deFile
          .dropRight(16)
      )

      val deFile2 = withFile0("test/printing/res/de-print2.md")(identity)
      val mc2 = module.copy(metadata =
        module.metadata.copy(workload =
          module.metadata.workload.copy(
            lecture = 15,
            practical = 0
          )
        )
      )
      assert(
        dePrinter
          .print(mc2, new StringBuilder())
          .value
          .toString()
          .dropRight(16) == deFile2
          .dropRight(17)
      )

      val deFile3 = withFile0("test/printing/res/de-print3.md")(identity)
      val mc3 = module.copy(metadata =
        module.metadata.copy(workload =
          module.metadata.workload.copy(
            lecture = 150,
            practical = 0,
            exercise = 0
          )
        )
      )
      assert(
        dePrinter
          .print(mc3, new StringBuilder())
          .value
          .toString()
          .dropRight(16) == deFile3
          .dropRight(16)
      )
    }
  }
}
