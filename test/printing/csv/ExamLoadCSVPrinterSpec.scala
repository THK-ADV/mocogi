package printing.csv

import java.util.UUID

import cats.data.NonEmptyList
import models.*
import models.core.AssessmentMethod
import models.core.ExamPhases.ExamPhase
import models.core.ModuleType
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import parsing.types.ModuleContent

final class ExamLoadCSVPrinterSpec extends AnyWordSpec with Matchers {

  private val header = Vector(
    "Semester",
    "Modul",
    "Modulnummer",
    "Teilmodule",
    "Modulart",
    "ECTS Teilmodul",
    "ECTS Gesamt",
    "Anwesenheitspflicht ja / nein",
    "Anwesenheitspflicht wenn ja, Mindestpräsenzzeit",
    "Anwesenheitspflicht wenn ja, Begründung",
    "Prüfungsvorleistung ja / nein",
    "Prüfungsvorleistung wenn ja, welche(s) (Teil)Modul(e)",
    "Prüfungsvorleistung wenn ja, Begründung",
    "Prüfungsformen / Gewichtung / Benotung",
    "Prüfungsleistungen pro (Teil)Modul",
  )

  private val emptyContent = ModuleContent("", "", "", "", "")

  private def id(value: Long) = new UUID(0, value)

  private def metadata(
      title: String = "Module",
      abbrev: String = "MOD",
      moduleType: String = "module",
      ects: Double = 5,
      moduleRelation: Option[ModuleRelationProtocol] = None,
      attendanceRequirement: Option[AttendanceRequirement] = None,
      assessmentPrerequisite: Option[AssessmentPrerequisite] = None
  ) =
    MetadataProtocol(
      title = title,
      abbrev = abbrev,
      moduleType = moduleType,
      ects = ects,
      language = "de",
      duration = 1,
      season = "summer",
      workload = ModuleWorkload(0, 0, 0, 0, 0, 0),
      status = "active",
      location = "location",
      participants = None,
      moduleRelation = moduleRelation,
      moduleManagement = NonEmptyList.one("module-management"),
      lecturers = NonEmptyList.one("lecturer"),
      assessmentMethods = ModuleAssessmentMethodsProtocol(Nil),
      examiner = Examiner.NN,
      examPhases = NonEmptyList.one(ExamPhase.none.id),
      prerequisites = ModulePrerequisitesProtocol(None, None),
      po = ModulePOProtocol(Nil, Nil),
      taughtWith = Nil,
      attendanceRequirement = attendanceRequirement,
      assessmentPrerequisite = assessmentPrerequisite
    )

  private def examLoadModule(
      moduleId: UUID,
      moduleMetadata: MetadataProtocol = metadata(),
      semesters: List[Int] = List(1)
  ) =
    ExamLoadModule(moduleId, moduleMetadata, semesters)

  private def childModule(moduleId: UUID, moduleMetadata: MetadataProtocol) =
    moduleId -> ModuleProtocol(Some(moduleId), moduleMetadata, emptyContent, emptyContent)

  private def printExamLoad(
      modules: Vector[ExamLoadModule],
      childrenById: Map[UUID, ModuleProtocol] = Map.empty,
      assessmentMethods: Map[UUID, Seq[AssessmentMethod]] = Map.empty,
      electiveGroups: Vector[ElectiveGroup] = Vector.empty
  ) =
    new ExamLoadCSVPrinter(modules, childrenById, assessmentMethods, electiveGroups).print()

  private def dataRows(csv: String) =
    csv.linesIterator.drop(1).map(_.split(";", -1).toVector).toVector

  private def onlyDataRow(csv: String) = {
    val rows = dataRows(csv)
    rows should have size 1
    rows.head
  }

  private def assessmentColumns(methods: Option[Seq[AssessmentMethod]]) = {
    val moduleId        = id(100)
    val methodsByModule = methods match {
      case Some(values) => Map(moduleId -> values)
      case None         => Map.empty[UUID, Seq[AssessmentMethod]]
    }
    onlyDataRow(printExamLoad(Vector(examLoadModule(moduleId)), assessmentMethods = methodsByModule)).slice(13, 15)
  }

  "ExamLoadCSVPrinter" should {
    "print the exact header and no data rows when there are no modules" in {
      printExamLoad(Vector.empty) shouldBe header.mkString(";")
    }

    "render an ordinary mandatory module with the default requirement values" in {
      val moduleId = id(1)
      val module   = examLoadModule(
        moduleId,
        metadata(
          title = "Algorithmen und Programmierung 1",
          abbrev = "AP1",
          ects = 8
        )
      )
      val methods = Map(moduleId -> Seq(AssessmentMethod("term-paper", "Hausarbeit", "Term paper")))

      onlyDataRow(printExamLoad(Vector(module), assessmentMethods = methods)) shouldBe Vector(
        "1",
        "Algorithmen und Programmierung 1",
        "AP1",
        "-",
        "PF",
        "8",
        "8",
        "nein",
        "-",
        "-",
        "nein",
        "-",
        "-",
        "Hausarbeit",
        "1",
      )
    }

    "render and trim attendance requirements and assessment prerequisites" in {
      val module = examLoadModule(
        id(2),
        metadata(
          title = "Mensch-Computer-Interaktion",
          abbrev = "MCI",
          ects = 6,
          attendanceRequirement = Some(
            AttendanceRequirement(
              " 4/7 Termine ",
              " Kleingruppenarbeit und / oder Meilensteinabnahmen ",
              "unberücksichtigt"
            )
          ),
          assessmentPrerequisite = Some(
            AssessmentPrerequisite(
              " Teilnahme an den Meilensteinprüfungen ",
              " Praktische Erfahrung als Grundlage der Prüfung. "
            )
          )
        )
      )

      onlyDataRow(printExamLoad(Vector(module))).slice(7, 13) shouldBe Vector(
        "ja",
        "4/7 Termine",
        "Kleingruppenarbeit und / oder Meilensteinabnahmen",
        "ja",
        "Teilnahme an den Meilensteinprüfungen",
        "Praktische Erfahrung als Grundlage der Prüfung.",
      )
    }

    "render generic modules as WPF with multiple semesters and decimal ECTS" in {
      val module = examLoadModule(
        id(3),
        metadata(
          title = "Wahlmodul (Vertiefung)",
          abbrev = "WPF-V",
          moduleType = ModuleType.genericId,
          ects = 2.5
        ),
        List(5, 6)
      )

      val row = onlyDataRow(printExamLoad(Vector(module)))

      row(0) shouldBe "5,6"
      row(4) shouldBe "WPF"
      row.slice(5, 7) shouldBe Vector("2,5", "2,5")
    }

    "render a module without semesters with an empty semester field" in {
      val module = examLoadModule(
        id(4),
        metadata(title = "Module without semester"),
        Nil
      )

      onlyDataRow(printExamLoad(Vector(module))).take(2) shouldBe Vector("", "Module without semester")
    }

    "render children below their parent, sorted by title, and use each module's assessment methods" in {
      val parentId  = id(6)
      val societyId = id(7)
      val lawId     = id(8)

      val parentMetadata = metadata(
        title = "Informatik, Recht und Gesellschaft",
        abbrev = "IRG",
        ects = 5,
        moduleRelation = Some(ModuleRelationProtocol(NonEmptyList.of(lawId, societyId)))
      )
      val societyMetadata = metadata(
        title = "Informatik und Gesellschaft",
        abbrev = "IUG",
        ects = 3
      )
      val lawMetadata = metadata(
        title = "Recht",
        abbrev = "RE",
        ects = 2
      )

      val modules  = Vector(examLoadModule(parentId, parentMetadata, List(4)))
      val children = Map(
        childModule(lawId, lawMetadata),
        childModule(societyId, societyMetadata)
      )
      val methods = Map(
        parentId  -> Seq(AssessmentMethod("term-paper", "Hausarbeit", "Term paper")),
        societyId -> Seq(AssessmentMethod("oral-exam", "Mündliche Prüfung", "Oral examination")),
        lawId     -> Seq(AssessmentMethod("written-exam", "Klausurarbeiten", "Written examination"))
      )

      val rows = dataRows(printExamLoad(modules, children, methods))

      rows.map(_(1)) shouldBe Vector(
        "Informatik, Recht und Gesellschaft",
        "Informatik und Gesellschaft",
        "Recht",
      )
      rows.map(row => (row(0), row(3), row(5), row(6))) shouldBe Vector(
        ("4", "", "", "5"),
        ("", "-", "3", ""),
        ("", "-", "2", ""),
      )
      rows.map(_(13)) shouldBe Vector("Hausarbeit", "Mündliche Prüfung", "Klausurarbeit")
      rows.map(_(14)) shouldBe Vector("1", "1", "1")
    }

    "keep rendering a parent when a referenced child is unavailable" in {
      val parentId = id(60)
      val missing  = id(61)
      val parent   = examLoadModule(
        parentId,
        metadata(
          title = "Parent",
          moduleRelation = Some(ModuleRelationProtocol(NonEmptyList.one(missing)))
        )
      )

      onlyDataRow(printExamLoad(Vector(parent))).apply(1) shouldBe "Parent"
    }

    "append elective groups after mandatory modules with generic title delimiters" in {
      val mandatory = examLoadModule(id(10), metadata(title = "Pflichtmodul", abbrev = "PF1"))
      val electiveA = examLoadModule(id(11), metadata(title = "Elective A", abbrev = "EA"), List(3))
      val electiveB = examLoadModule(id(12), metadata(title = "Elective B", abbrev = "EB"), List(2))
      val groups    = Vector(
        ElectiveGroup("Wahlmodul B", Vector(electiveB)),
        ElectiveGroup("Wahlmodul A", Vector(electiveA)),
      )

      val rows = dataRows(printExamLoad(Vector(mandatory), electiveGroups = groups))

      rows.map(_(1)) shouldBe Vector("Pflichtmodul", "Wahlmodul B", "Elective B", "Wahlmodul A", "Elective A")
      rows(1) shouldBe Vector(
        "k. A.",
        "Wahlmodul B",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A.",
        "k. A."
      )
      rows(2)(4) shouldBe "WPF"
      rows(4)(4) shouldBe "WPF"
    }

    "label every written exam method as Klausurarbeit" in {
      val writtenExamIds =
        Seq("e-exam", "written-exam", "written-exam-answer-choice-method")

      writtenExamIds.foreach { methodId =>
        val method = AssessmentMethod(methodId, s"Original label for $methodId", "English label")

        assessmentColumns(Some(Seq(method))) shouldBe Vector("Klausurarbeit", "1")
      }
    }

    "merge written exam methods while retaining and sorting other methods" in {
      val methods = Seq(
        AssessmentMethod("written-exam", "Klausurarbeiten", "Written examination"),
        AssessmentMethod("oral-exam", "Mündliche Prüfung", "Oral examination"),
        AssessmentMethod("e-exam", "Elektronische Prüfung", "Electronic examination"),
        AssessmentMethod(
          "written-exam-answer-choice-method",
          "Schriftliche Prüfung im Antwortwahlverfahren",
          "Written multiple-choice examination"
        ),
        AssessmentMethod(
          "written-exam-extra",
          "Andere schriftliche Prüfung",
          "Other written examination"
        ),
      )

      assessmentColumns(Some(methods)) shouldBe Vector(
        "Andere schriftliche Prüfung und/oder Klausurarbeit und/oder Mündliche Prüfung",
        "3",
      )
    }

    "render a stable empty value when assessment methods are missing or empty" in {
      assessmentColumns(None) shouldBe Vector("-", "0")
      assessmentColumns(Some(Seq.empty)) shouldBe Vector("-", "0")
    }

    "quote values containing CSV delimiters, quotes, or line breaks" in {
      val moduleId = id(9)
      val module   = examLoadModule(
        moduleId,
        metadata(
          title = "Policy; Ethics",
          abbrev = "MOD\"X",
          assessmentPrerequisite = Some(
            AssessmentPrerequisite("Praktikum", "Erste Zeile\nZweite Zeile")
          )
        )
      )
      val escapedTitle  = "\"Policy; Ethics\""
      val escapedAbbrev = "\"MOD\"\"X\""
      val escapedReason = "\"Erste Zeile\nZweite Zeile\""
      val expectedRow   = Vector(
        "1",
        escapedTitle,
        escapedAbbrev,
        "-",
        "PF",
        "5",
        "5",
        "nein",
        "-",
        "-",
        "ja",
        "Praktikum",
        escapedReason,
        "-",
        "0",
      ).mkString(";")

      printExamLoad(Vector(module)) shouldBe s"${header.mkString(";")}\n$expectedRow"
    }
  }
}
