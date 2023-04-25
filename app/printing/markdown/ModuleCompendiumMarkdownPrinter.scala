package printing.markdown

import models.core.Person
import parsing.types._
import printer.Printer
import printer.Printer.{newline, prefix}
import printing.PrintingLanguage
import printing.markdown.ModuleCompendiumPrinter.{LanguageOps, StringConcatOps}
import service.core.StudyProgramShort
import validator.{Metadata, ModuleRelation, POs, PrerequisiteEntry}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ModuleCompendiumMarkdownPrinter extends ModuleCompendiumPrinter {

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

  private def row(key: String, value: String): Printer[Unit] =
    prefix(s"| $key | $value |")
      .skip(newline)

  private def rows(key: String, value: List[String]): Printer[Unit] =
    value.zipWithIndex
      .map { case (s, i) =>
        row(if (i == 0) key else "", s)
      }
      .reduce(_ skip _)

  private def fmtPeople(label: String, xs: List[Person]): Printer[Unit] = {
    def fmt(x: Person) = x match {
      case s: Person.Single =>
        s"${s.title} ${s.firstname} ${s.lastname} (${fmtCommaSeparated(s.faculties)(_.abbrev)})"
      case g: Person.Group =>
        g.title
      case u: Person.Unknown =>
        u.title
    }
    rows(label, xs.map(fmt))
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
        val text =
          nonEmptyRow(e.text)(s => s"${lang.prerequisitesTextLabel}: $s")
        val modules = nonEmptyRow(fmtCommaSeparated(e.modules)(_.abbrev))(s =>
          s"${lang.prerequisitesModuleLabel}: $s"
        )
        val pos = nonEmptyRow(fmtCommaSeparated(e.pos)(_.abbrev))(s =>
          s"${lang.prerequisitesStudyProgramLabel}: $s"
        )
        text
          .skip(modules)
          .skip(pos)
    }

  private def fmtPOs(
      label: String,
      pos: POs,
      studyProgram: String => Option[StudyProgramShort]
  )(implicit
      lang: PrintingLanguage
  ): Printer[Unit] = {
    def fmt(p: POMandatory) = {
      val semester =
        s"(${lang.semesterLabel} ${fmtCommaSeparated(p.recommendedSemester)(_.toString)})"
      val studyProgramWithPO = studyProgram(p.po.program) match {
        case Some(sp) =>
          val spLabel = lang.fold(sp.deLabel, sp.enLabel)
          val gradeLabel = lang.fold(sp.grade.deLabel, sp.grade.enLabel)
          s"$gradeLabel: $spLabel PO ${p.po.version}"
        case None =>
          p.po.abbrev
      }
      s"$studyProgramWithPO $semester"
    }

    val xs = pos.mandatory
    if (xs.isEmpty) row(label, lang.noneLabel)
    else rows(label, xs.map(fmt))
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
      label: String,
      ams: AssessmentMethods
  )(implicit lang: PrintingLanguage): Printer[Unit] =
    rows(
      label,
      ams.mandatory
        .map { am =>
          val methodValue = lang.value(am.method)
          am.percentage.fold(methodValue)(d =>
            s"$methodValue (${fmtDouble(d)} %)"
          )
        }
    )

  private def header(title: String) =
    prefix(s"## $title").skip(newline)

  private def contentBlock(title: String, content: String) =
    header(title)
      .skip(
        if (content.isEmpty) newline
        else
          newline
            .skip(prefix(content))
            .skip(newline.repeat(2))
      )

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
    row(language.durationLabel, s"${m.duration} ${language.semesterLabel}")

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
    fmtAssessmentMethod(
      language.assessmentMethodLabel,
      m.assessmentMethods
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

  private def pos(studyProgram: String => Option[StudyProgramShort])(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    fmtPOs(language.poLabel, m.validPOs, studyProgram)

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

  private def lastModified(implicit
      lang: PrintingLanguage,
      localDateTime: LocalDateTime
  ) =
    prefix(
      s"${lang.lastModifiedLabel} ${localDateTime.format(localDatePattern)}"
    )

  private def header(implicit m: Metadata) =
    prefix("# ")
      .skip(prefix(m.title))

  private def particularities(implicit c: Content, lang: PrintingLanguage) =
    contentBlock(lang.particularitiesLabel, c.particularities)

  private def recommendedReading(implicit c: Content, lang: PrintingLanguage) =
    contentBlock(lang.recommendedReadingLabel, c.recommendedReading)

  private def teachingAndLearningMethods(implicit
      c: Content,
      lang: PrintingLanguage
  ) =
    contentBlock(
      lang.teachingAndLearningMethodsLabel,
      c.teachingAndLearningMethods
    )

  private def moduleContent(implicit c: Content, lang: PrintingLanguage) =
    contentBlock(lang.moduleContentLabel, c.content)

  private def learningOutcome(implicit c: Content, lang: PrintingLanguage) =
    contentBlock(lang.learningOutcomeLabel, c.learningOutcome)

  override def printer(studyProgram: String => Option[StudyProgramShort])(
      implicit
      lang: PrintingLanguage,
      localDateTime: LocalDateTime
  ): Printer[ModuleCompendium] =
    Printer { case (mc, input) =>
      implicit val m: Metadata = mc.metadata
      implicit val c: Content = content(mc)

      header
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
        .skip(pos(studyProgram))
        .skip(newline)
        .skip(learningOutcome)
        .skip(moduleContent)
        .skip(teachingAndLearningMethods)
        .skip(recommendedReading)
        .skip(particularities)
        .skip(prefix("---"))
        .skip(newline.repeat(2))
        .skip(lastModified)
        .print((), input)
    }
}
