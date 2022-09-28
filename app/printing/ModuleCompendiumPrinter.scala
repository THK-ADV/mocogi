package printing

import controllers.parameter.PrinterOutputFormat
import parsing.types._
import printer.Printer
import printer.Printer._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Singleton
import scala.language.implicitConversions

trait ModuleCompendiumPrinter {
  def printerForFormat(
      outputFormat: PrinterOutputFormat
  ): Printer[ModuleCompendium]
}

@Singleton
class ModuleCompendiumPrinterImpl extends ModuleCompendiumPrinter {

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  private def now: String = LocalDate.now().format(localDatePattern)

  private def row(key: String, value: String): Printer[Unit] =
    prefix(s"| $key | $value |")
      .skip(newline)

  private def fmtPeople(p: Person): String = p.abbrev match {
    case "all" | "nn" => p.lastname
    case _ => s"${p.title} ${p.firstname} ${p.lastname} (${p.faculty})"
  }

  private def fmtPrerequisites(prerequisites: Option[Prerequisites]): String =
    prerequisites match {
      case None    => "Keine"
      case Some(p) => p.modules.mkString("\n") // TODO use all fields
    }

  private def fmtPOMandatory(xs: List[POMandatory]): String =
    if (xs.isEmpty) "Keine"
    else xs.map(_.studyProgram).mkString(", ") // TODO use all fields

  private def moduleRelationRow(relation: ModuleRelation): Printer[Unit] =
    relation match {
      case ModuleRelation.Parent(children) =>
        row("Besteht aus den Teilmodulen", children.mkString(", "))
      case ModuleRelation.Child(parent) =>
        row("Gehört zum Modul", parent)
    }

  private def fmtDouble(d: Double): String =
    if (d % 1 == 0) d.toInt.toString
    else d.toString.replace('.', ',')

  // TODO use all fields
  private def fmtAssessmentMethod(am: AssessmentMethodEntry): String =
    am.percentage.fold(am.method.deLabel)(d =>
      s"${am.method.deLabel} (${fmtDouble(d)} %)"
    )

  private def header(title: String) =
    prefix(s"## $title")
      .skip(newline)

  private def contentBlock(title: String, content: String) =
    header(title).skip(prefix(content))

  private def linkToHeader(header: String): String =
    s"[Siehe $header](#${header.toLowerCase.replace(' ', '-')})"

  override def printerForFormat(
      outputFormat: PrinterOutputFormat
  ): Printer[ModuleCompendium] =
    outputFormat match {
      case PrinterOutputFormat.DefaultPrinter => defaultPrinter
    }

  val defaultPrinter: Printer[ModuleCompendium] = Printer { case (mc, input) =>
    val m = mc.metadata
    val c = mc.deContent

    prefix("#")
      .skip(whitespace)
      .skip(prefix(m.title))
      .skip(newline.repeat(2))
      .skip(row("", ""))
      .skip(row("---", "---"))
      .skip(row("Modulnummer", m.abbrev))
      .skip(row("Modulbezeichnung", m.title))
      .skip(row("Art des Moduls", m.kind.deLabel))
      .skipOpt(m.relation.map(moduleRelationRow))
      .skip(row("ECTS credits", fmtDouble(m.credits.value)))
      .skip(row("Sprache", m.language.de_label))
      .skip(row("Dauer des Moduls", s"${m.duration} Semester"))
      .skip(row("Häufigkeit des Angebots", s"Jedes ${m.frequency.deLabel}"))
      .skip(
        row(
          "Modulverantwortliche*r",
          m.responsibilities.moduleManagement.map(fmtPeople).mkString(", ")
        )
      )
      .skip(
        row(
          "Dozierende",
          m.responsibilities.lecturers.map(fmtPeople).mkString(", ")
        )
      )
      .skip(
        row(
          "Prüfungsformen",
          m.assessmentMethodsMandatory.map(fmtAssessmentMethod).mkString(", ")
        )
      )
      .skip(row("Workload", ""))
      .skip(row("\tVorlesung", s"${m.workload.lecture} h"))
      .skip(row("\tSeminar", s"${m.workload.seminar} h"))
      .skip(row("\tPraktikum", s"${m.workload.practical} h"))
      .skip(row("\tÜbung", s"${m.workload.exercise} h"))
      .skip(row("\tProjektbetreuung", s"${m.workload.projectSupervision} h"))
      .skip(row("\tProjektarbeit", s"${m.workload.projectWork} h"))
      .skip(
        row(
          "Empfohlene Voraussetzungen",
          fmtPrerequisites(m.recommendedPrerequisites)
        )
      )
      .skip(
        row(
          "Zwingende Voraussetzungen",
          fmtPrerequisites(m.requiredPrerequisites)
        )
      )
      .skip(
        row(
          "Zusätzliche Voraussetzungen",
          if (c.recommendedPrerequisitesBody.trim.nonEmpty)
            linkToHeader(c.recommendedPrerequisitesHeader)
          else "Keine️"
        )
      )
      .skip(
        row(
          "Verwendung des Moduls in weiteren Studiengängen",
          fmtPOMandatory(m.poMandatory)
        )
      )
      .skip(newline)
      .skip(contentBlock(c.learningOutcomeHeader, c.learningOutcomeBody))
      .skip(contentBlock(c.contentHeader, c.contentBody))
      .skip(
        contentBlock(
          c.teachingAndLearningMethodsHeader,
          c.teachingAndLearningMethodsBody
        )
      )
      .skip(
        contentBlock(c.recommendedReadingHeader, c.recommendedReadingBody)
      )
      .skip(
        contentBlock(
          c.recommendedPrerequisitesHeader,
          c.recommendedPrerequisitesBody
        )
      )
      .skip(contentBlock(c.particularitiesHeader, c.particularitiesBody))
      .skip(prefix("---"))
      .skip(newline.repeat(2))
      .skip(prefix(s"Letzte Aktualisierung am $now"))
      .print((), input)
  }

}
