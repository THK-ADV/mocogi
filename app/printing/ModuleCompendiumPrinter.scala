package printing

import ops.EitherOps.EOps
import parsing.ModuleCompendiumParser
import parsing.types._
import printer.Printer
import printer.Printer._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.language.implicitConversions

trait ModuleCompendiumPrinter {
  def renderOutput(
      input: String,
      outputFormat: PrinterOutputFormat
  ): Either[ModuleCompendiumGenerationError, PrinterOutput]
}

@Singleton
class ModuleCompendiumPrinterImpl @Inject() (
    moduleCompendiumParser: ModuleCompendiumParser,
    markdownConverter: MarkdownConverter
) extends ModuleCompendiumPrinter {

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  private def now: String = LocalDate.now().format(localDatePattern)

  private def row(key: String, value: String): Printer[Unit] =
    prefix(s"| $key | $value |")
      .skip(newline)

  private def fmtPeople(p: People): String = p.abbrev match {
    case "all" | "nn" => p.lastname
    case _ => s"${p.title} ${p.firstname} ${p.lastname} (${p.faculty})"
  }

  private def fmtPrerequisites(xs: List[String]): String =
    if (xs.isEmpty) "Keine"
    else xs.mkString(", ")

  private def fmtStudyPrograms(xs: List[String]): String =
    if (xs.isEmpty) "Keine"
    else xs.mkString(", ")

  private def fmtModuleRelation(relation: ModuleRelation): String =
    relation match {
      case ModuleRelation.Parent(children) =>
        s"Submodule: ${children.mkString(", ")}"
      case ModuleRelation.Child(parent) =>
        s"Supermodul: $parent"
    }

  private def fmtDouble(d: Double): String =
    if (d % 1 == 0) d.toInt.toString
    else d.toString.replace('.', ',')

  private def fmtAssessmentMethod(am: AssessmentMethodPercentage): String =
    am.percentage.fold(am.assessmentMethod.deLabel)(d =>
      s"${am.assessmentMethod.deLabel} (${fmtDouble(d)} %)"
    )

  private def header(title: String) =
    prefix(s"## $title")
      .skip(newline)

  private def contentBlock(title: String, content: String) =
    header(title).skip(prefix(content))

  val printer: Printer[ModuleCompendium] = Printer { case (mc, input) =>
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
      .skipOpt(
        m.relation.map(i => row("Modulbeziehung", fmtModuleRelation(i)))
      )
      .skip(row("ECTS credits", fmtDouble(m.credits)))
      .skip(row("Sprache", m.language.de_label))
      .skip(row("Dauer des Moduls", s"${m.duration} Semester"))
      .skip(
        row("Empfohlenes Studiensemester", m.recommendedSemester.toString)
      )
      .skip(row("Häufigkeit des Angebots", s"Jedes ${m.frequency.deLabel}"))
      .skip(
        row(
          "Modulverantwortliche*r",
          m.responsibilities.coordinators.map(fmtPeople).mkString(", ")
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
          m.assessmentMethods.map(fmtAssessmentMethod).mkString(", ")
        )
      )
      .skip(row("Workload", s"${m.workload.total} h"))
      .skip(row("  Vorlesung", s"${m.workload.lecture} h"))
      .skip(row("  Seminar", s"${m.workload.seminar} h"))
      .skip(row("  Praktikum", s"${m.workload.practical} h"))
      .skip(row("  Übung", s"${m.workload.exercise} h"))
      .skip(row("  Selbststudium", s"${m.workload.selfStudy} h"))
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
          "Verwendung des Moduls in weiteren Studiengängen",
          fmtStudyPrograms(m.po)
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

  def renderOutput(
      input: String,
      outputFormat: PrinterOutputFormat
  ): Either[ModuleCompendiumGenerationError, PrinterOutput] =
    moduleCompendiumParser.parser
      .parse(input)
      ._1
      .biFlatMap(
        ModuleCompendiumGenerationError.Parsing.apply,
        ModuleCompendiumGenerationError.Printing.apply,
        printer.print(_, "")
      )
      .biFlatMap(
        identity,
        ModuleCompendiumGenerationError.Other.apply,
        input => markdownConverter.convert(input, outputFormat)
      )
}
