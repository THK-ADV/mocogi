package printing.markdown

import models.core.Person
import parsing.types._
import printer.Printer
import printer.Printer.{newline, prefix}
import printing.{PrintingLanguage, StringConcatOps, fmtDouble, localDatePattern}
import service.core.StudyProgramShort
import validator.{Metadata, ModuleRelation, POs, PrerequisiteEntry}

import java.time.LocalDateTime
import javax.inject.Singleton

@Singleton
final class ModuleCompendiumMarkdownPrinter(
    private val substituteLocalisedContent: Boolean
) {

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
      case s: Person.Default =>
        s"${s.title} ${s.firstname} ${s.lastname} (${fmtCommaSeparated(s.faculties)(_.abbrev)})"
      case g: Person.Group =>
        g.label
      case u: Person.Unknown =>
        u.label
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
      val semester = Option.when(p.recommendedSemester.nonEmpty)(
        s"(${lang.semesterLabel} ${fmtCommaSeparated(p.recommendedSemester)(_.toString)})"
      )
      val studyProgramWithPO = studyProgram(p.po.program) match {
        case Some(sp) =>
          val spLabel = lang.fold(sp.deLabel, sp.enLabel)
          val gradeLabel = lang.fold(sp.grade.deLabel, sp.grade.enLabel)
          s"$gradeLabel: $spLabel PO ${p.po.version}"
        case None =>
          p.po.abbrev
      }
      semester.fold(studyProgramWithPO)(s => s"$studyProgramWithPO $s")
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

  def contentBlock(title: String, de: String, en: String)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) = {
    val content = {
      val l = lang.fold(de, en)
      if (l.isEmpty && substituteLocalisedContent) lang.fold(en, de) else l
    }
    val contentPrinter =
      if (content.isEmpty) newline
      else
        newline
          .skip(prefix(content))
          .skip(newline.repeat(2))
    header(title).skip(contentPrinter)
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
    val contactHoursValueLabel =
      if (parts.isEmpty) s"$contactHoursValue h"
      else s"$contactHoursValue h ($parts)"
    val selfStudyLabel =
      if (wl.selfStudy == 0) language.noneLabel
      else s"${wl.selfStudy} h"
    row(language.workloadLabel, s"${wl.total} h")
      .skip(row(language.contactHoursLabel, contactHoursValueLabel))
      .skip(row(language.selfStudyLabel, selfStudyLabel))
  }

  private def lastModified(implicit
      lang: PrintingLanguage,
      localDateTime: LocalDateTime
  ) =
    prefix(
      s"${lang.lastModifiedLabel} ${localDateTime.format(localDatePattern)}"
    )

  private def header(implicit m: Metadata) =
    prefix("# ").skip(prefix(m.title))

  private def particularities(de: Content, en: Content)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.particularitiesLabel,
      de.particularities,
      en.particularities
    )

  private def recommendedReading(de: Content, en: Content)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.recommendedReadingLabel,
      de.recommendedReading,
      en.recommendedReading
    )

  private def teachingAndLearningMethods(de: Content, en: Content)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.teachingAndLearningMethodsLabel,
      de.teachingAndLearningMethods,
      en.teachingAndLearningMethods
    )

  private def moduleContent(de: Content, en: Content)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(lang.moduleContentLabel, de.content, en.content)

  private def learningOutcome(de: Content, en: Content)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.learningOutcomeLabel,
      de.learningOutcome,
      en.learningOutcome
    )

  def printer(studyProgram: String => Option[StudyProgramShort])(implicit
      lang: PrintingLanguage,
      localDateTime: LocalDateTime
  ): Printer[ModuleCompendium] =
    Printer { case (mc, input) =>
      implicit val m: Metadata = mc.metadata
      implicit val slc: Boolean = substituteLocalisedContent

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
        .skip(learningOutcome(mc.deContent, mc.enContent))
        .skip(moduleContent(mc.deContent, mc.enContent))
        .skip(teachingAndLearningMethods(mc.deContent, mc.enContent))
        .skip(recommendedReading(mc.deContent, mc.enContent))
        .skip(particularities(mc.deContent, mc.enContent))
        .skip(prefix("---"))
        .skip(newline.repeat(2))
        .skip(lastModified)
        .print((), input)
    }
}
