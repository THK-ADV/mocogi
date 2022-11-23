package printing

import basedata.Person
import parsing.types._
import printer.Printer
import printer.Printer.{newline, prefix, whitespace}
import printing.ModuleCompendiumPrinter.{LanguageOps, StringConcatOps}
import validator.{Metadata, ModuleRelation, POs, PrerequisiteEntry}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

object ModuleCompendiumDefaultPrinter extends ModuleCompendiumPrinter {

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

  private def row(key: String, value: String): Printer[Unit] =
    prefix(s"| $key | $value |")
      .skip(newline)

  private def fmtPeople(label: String, xs: List[Person]): Printer[Unit] = {
    def go(x: Person) = x match {
      case s: Person.Single =>
        s"${s.title} ${s.firstname} ${s.lastname} (${fmtCommaSeparated(s.faculties)(_.abbrev)})"
      case g: Person.Group =>
        g.title
      case u: Person.Unknown =>
        u.title
    }

    xs.map(go)
      .zipWithIndex
      .map { case (s, i) =>
        row(if (i == 0) label else "", s)
      }
      .reduce(_ skip _) // TODO this seems to be a good pattern
  }

  private def fmtCommaSeparated[A](xs: List[A])(f: A => String): String =
    xs.map(f).mkString(", ")

  private def nonEmptyRow(
      value: String
  )(modify: String => String): Printer[Unit] =
    if (value.nonEmpty) row("", modify(value))
    else Printer.always()

  private def fmtPrerequisites(
      label: String,
      entry: Option[PrerequisiteEntry]
  )(implicit lang: PrintingLanguage): Printer[Unit] =
    entry match {
      case None =>
        row(label, lang.noneLabel)
      case Some(e) if e.modules.isEmpty || e.text.isEmpty || e.pos.isEmpty =>
        row(label, lang.noneLabel)
      case Some(e) =>
        val text = nonEmptyRow(e.text)(s => s"Beschreibung: $s")
        val modules = nonEmptyRow(fmtCommaSeparated(e.modules)(_.abbrev))(s =>
          s"Module: $s"
        )
        val pos = nonEmptyRow(fmtCommaSeparated(e.pos)(_.abbrev))(s =>
          s"Studiengänge: $s"
        )
        text
          .skip(modules)
          .skip(pos)
    }

  private def fmtPOs(label: String, pos: POs)(implicit
      lang: PrintingLanguage
  ): Printer[Unit] = {
    val xs = pos.mandatory
    if (xs.isEmpty) row(label, lang.noneLabel)
    else
      xs.map(p =>
        s"${p.po.abbrev} (${fmtCommaSeparated(p.recommendedSemester)(_.toString)})"
      ).zipWithIndex
        .map { case (s, i) =>
          row(if (i == 0) label else "", s)
        }
        .reduce(_ skip _) // TODO this seems to be a good pattern
  }

  private def fmtModuleRelation(
      relation: ModuleRelation
  )(implicit lang: PrintingLanguage): Printer[Unit] =
    relation match {
      case ModuleRelation.Parent(children) =>
        row(
          lang.parentLabel,
          fmtCommaSeparated(children)(_.abbrev)
        )
      case ModuleRelation.Child(parent) =>
        row(lang.childLabel, parent.abbrev)
    }

  private def fmtDouble(d: Double): String =
    if (d % 1 == 0) d.toInt.toString
    else d.toString.replace('.', ',')

  private def fmtAssessmentMethod(
      ams: AssessmentMethods
  )(implicit lang: PrintingLanguage): String =
    ams.mandatory
      .map { am =>
        val methodValue = lang.value(am.method)
        am.percentage.fold(methodValue)(d =>
          s"$methodValue (${fmtDouble(d)} %)"
        )
      }
      .mkString(", ")

  private def header(title: String) =
    prefix(s"## $title")
      .skip(newline)

  private def contentBlock(title: String, content: String) =
    header(title).skip(prefix(content))

  private def linkToHeader(header: String): String =
    s"[Siehe $header](#${header.toLowerCase.replace(' ', '-')})"

  private def content(
      mc: ModuleCompendium
  )(implicit language: PrintingLanguage) =
    language match {
      case PrintingLanguage.German  => mc.deContent
      case PrintingLanguage.English => mc.enContent
    }

  private def moduleNumber(implicit m: Metadata, language: PrintingLanguage) =
    row(language.moduleCodeLabel, m.abbrev)

  private def moduleTitle(implicit m: Metadata, language: PrintingLanguage) =
    row(language.moduleTitleLabel, m.title)

  private def moduleType(implicit m: Metadata, language: PrintingLanguage) =
    row(language.moduleTypeLabel, language.value(m.kind))

  private def ects(implicit m: Metadata, language: PrintingLanguage) =
    row(language.ectsLabel, fmtDouble(m.ects.value))

  private def language(implicit m: Metadata, language: PrintingLanguage) =
    row(language.languageLabel, language.value(m.language))

  private def duration(implicit m: Metadata, language: PrintingLanguage) =
    row(language.durationLabel, s"${m.duration} Semester")

  private def frequency(implicit m: Metadata, language: PrintingLanguage) =
    row(language.frequencyLabel, language.frequencyValue(m.season))

  private def moduleCoordinator(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    fmtPeople(
      language.moduleCoordinatorLabel,
      m.responsibilities.moduleManagement
    )

  private def moduleLecturer(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    fmtPeople(
      language.lecturersLabel,
      m.responsibilities.lecturers
    )

  private def assessmentMethods(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    row(
      language.assessmentMethodLabel,
      fmtAssessmentMethod(m.assessmentMethods)
    )

  private def recommendedPrerequisites(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    fmtPrerequisites(
      language.recommendedPrerequisitesLabel,
      m.prerequisites.recommended
    )

  private def requiredPrerequisites(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    fmtPrerequisites(
      language.requiredPrerequisitesLabel,
      m.prerequisites.required
    )

  private def pos(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    fmtPOs(language.poLabel, m.validPOs)

  private def workload(implicit
      m: Metadata,
      language: PrintingLanguage
  ) = {
    val wl = m.workload
    val contactHoursValue = wl.total - wl.selfStudy
    val parts = language
      .lectureValue(wl)
      .combine(language.exerciseValue(wl))
      .combine(language.practicalValue(wl))
      .combine(language.seminarValue(wl))
      .combine(language.projectSupervisionValue(wl))
      .combine(language.projectWorkValue(wl))
    row(language.workloadLabel, s"${wl.total} h")
      .skip(row(language.contactHoursLabel, s"$contactHoursValue h ($parts)"))
      .skip(row(language.selfStudyLabel, s"${wl.selfStudy} h"))
  }

  override def printer(localDateTime: LocalDateTime)(implicit
      lang: PrintingLanguage
  ): Printer[ModuleCompendium] =
    Printer { case (mc, input) =>
      implicit val m: Metadata = mc.metadata
      implicit val c: Content = content(mc)

      prefix("#")
        .skip(whitespace)
        .skip(prefix(m.title))
        .skip(newline.repeat(2))
        .skip(row("", ""))
        .skip(row("---", "---"))
        .skip(moduleNumber)
        .skip(moduleTitle)
        .skip(moduleType)
        .skipOpt(m.relation.map(fmtModuleRelation))
        .skip(ects)
        .skip(language)
        .skip(duration)
        .skip(frequency)
        .skip(moduleCoordinator)
        .skip(moduleLecturer)
        .skip(assessmentMethods)
        .skip(workload)
        .skip(recommendedPrerequisites)
        .skip(requiredPrerequisites)
        .skip(pos)
        .skip(newline)
        .skip(learningOutcome)
        .skip(moduleContent)
        .skip(teachingAndLearningMethods)
        .skip(recommendedReading)
        .skip(particularities)
        .skip(prefix("---"))
        .skip(newline.repeat(2))
        .skip(lastModified(localDateTime))
        .print((), input)
    }

  private def lastModified(localDateTime: LocalDateTime) =
    prefix(s"Letzte Aktualisierung am ${localDateTime.format(localDatePattern)}")

  private def particularities(implicit c: Content) =
    contentBlock(c.particularitiesHeader, c.particularitiesBody)

  private def recommendedReading(implicit c: Content) =
    contentBlock(c.recommendedReadingHeader, c.recommendedReadingBody)

  private def teachingAndLearningMethods(implicit c: Content) =
    contentBlock(
      c.teachingAndLearningMethodsHeader,
      c.teachingAndLearningMethodsBody
    )

  private def moduleContent(implicit c: Content) =
    contentBlock(c.contentHeader, c.contentBody)

  private def learningOutcome(implicit c: Content) =
    contentBlock(c.learningOutcomeHeader, c.learningOutcomeBody)
}
