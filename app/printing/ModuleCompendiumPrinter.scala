package printing

import parsing.ModuleCompendiumParser
import parsing.types._
import printer.Printer
import printer.Printer._

import java.io.{ByteArrayInputStream, PrintWriter}
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try

object ModuleCompendiumPrinter {

  private def row(key: String, value: String): Printer[Unit] =
    prefix(s"| $key | $value |")
      .skip(newline)

  private def fmtPeople(p: People): String =
    s"${p.title} ${p.firstname} ${p.lastname} (${p.faculty})"

  private def fmtPrerequisites(xs: List[String]): String =
    if (xs.isEmpty) "Keine"
    else xs.mkString(", ")

  private def fmtStudyPrograms(xs: List[String]): String =
    if (xs.isEmpty) "Keine"
    else xs.mkString(", ")

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  private def now: String = LocalDate.now().format(localDatePattern)

  private def header(title: String) =
    prefix(s"## $title")
      .skip(newline)

  private def contentBlock(title: String, content: String) =
    header(title).skip(prefix(content))

  val moduleMetadataPrinter: Printer[ModuleCompendium] = Printer {
    case (mc, input) =>
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
        .skip(row("ECTS credits", m.credits.toString))
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
            m.assessmentMethod.map(_.deLabel).mkString(", ")
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
        .skip(contentBlock(c.recommendedReadingHeader, c.recommendedReadingBody))
        .skip(contentBlock(c.recommendedPrerequisitesHeader, c.recommendedPrerequisitesBody))
        .skip(contentBlock(c.particularitiesHeader, c.particularitiesBody))
        .skip(prefix("---"))
        .skip(newline.repeat(2))
        .skip(prefix(s"Letzte Aktualisierung am $now"))
        .print((), input)
  }

  def generate(input: String): Either[Throwable, String] = {
    val (res, rest) = ModuleCompendiumParser.moduleCompendiumParser.parse(input)
    if (rest.nonEmpty)
      Left(
        new Throwable(
          s"remaining input should be fully consumed, but was $rest"
        )
      )
    else
      res
        .flatMap(moduleMetadataPrinter.print(_, ""))
        .map(s => new ByteArrayInputStream(s.getBytes))
        .flatMap(input =>
          Try("pandoc -f markdown -t html" #< input !!).toEither
        )
  }

  def main(args: Array[String]): Unit = {
    def parse(name: String) =
      _root_.parsing
        .withResFile(s"$name.duda")(
          ModuleCompendiumParser.moduleCompendiumParser.parse
        )
        ._1
    def writeToFile(content: String, name: String): Unit = {
      val writer = new PrintWriter(
        s"res/$name.md",
        Charset.forName("utf-8")
      )
      writer.write(content)
      writer.close()
    }
    def createHtmlFile(path: String, name: String): Unit = {
      s"pandoc $path$name.md -f markdown -t html -s -o ${path}a.html" #&&
        s"open ${path}a.html" !
    }

    val name = "fsios"

    parse(name) match {
      case Right(mc) =>
        moduleMetadataPrinter.print(mc, "") match {
          case Right(s) =>
            val path = "/Users/alex/Developer/mocogi/res/"
            writeToFile(s, name)
            createHtmlFile(path, name)
          case Left(e) => throw e
        }
      case Left(e) => throw e
    }
  }
}
