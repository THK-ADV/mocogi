package printing.markdown

import cats.data.NonEmptyList
import models.*
import models.core.Identity
import parsing.types.*
import printer.Printer
import printer.Printer.{newline, prefix}
import printing.{
  LabelOps,
  LanguageOps,
  PrintingLanguage,
  fmtCommaSeparated,
  fmtDouble,
  fmtIdentity,
  localDatePattern
}

import java.time.LocalDateTime
import javax.inject.Singleton

@Singleton
final class ModuleMarkdownPrinter(
    private val substituteLocalisedContent: Boolean
) {

  private def row(key: String, value: String): Printer[Unit] =
    prefix(s"| $key | $value |")
      .skip(newline)

  private def rows(key: String, value: NonEmptyList[String]): Printer[Unit] =
    value.zipWithIndex
      .map { case (s, i) =>
        row(if (i == 0) key else "", s)
      }
      .reduceLeft(_ skip _)

  private def fmtPeople(
      label: String,
      xs: NonEmptyList[Identity]
  ): Printer[Unit] = {
    rows(label, xs.map(fmtIdentity))
  }

  private def nonEmptyRow(
      value: String
  )(modify: String => String): Printer[Unit] =
    if (value.nonEmpty) row("", modify(value))
    else Printer.always()

  private def fmtPrerequisites(
      label: String,
      entry: Option[ModulePrerequisiteEntry]
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
        val pos = nonEmptyRow(fmtCommaSeparated(e.pos)(_.id))(s =>
          s"${lang.prerequisitesStudyProgramLabel}: $s"
        )
        text
          .skip(modules)
          .skip(pos)
    }

  private def fmtPOs(
      label: String,
      pos: ModulePOs,
      studyProgram: String => Option[StudyProgramView]
  )(implicit
      lang: PrintingLanguage
  ): Printer[Unit] = {
    def fmt(p: ModulePOMandatory) = {
      val semester = Option.when(p.recommendedSemester.nonEmpty)(
        s"(${lang.semesterLabel} ${fmtCommaSeparated(p.recommendedSemester)(_.toString)})"
      )
      val studyProgramWithPO = studyProgram(p.po.program) match {
        case Some(sp) =>
          val spLabel = sp.localizedLabel
          val degreeLabel = sp.degree.localizedLabel
          s"$degreeLabel: $spLabel PO ${p.po.version}"
        case None =>
          p.po.id
      }
      semester.fold(studyProgramWithPO)(s => s"$studyProgramWithPO $s")
    }
    NonEmptyList
      .fromList(pos.mandatory)
      .fold(row(label, lang.noneLabel))(xs => rows(label, xs.map(fmt)))
  }

  private def fmtModuleRelation(
      relation: ModuleRelation
  )(implicit lang: PrintingLanguage): Printer[Unit] =
    relation match {
      case ModuleRelation.Parent(children) =>
        row(
          lang.parentLabel,
          fmtCommaSeparated(children.toList)(_.abbrev)
        )
      case ModuleRelation.Child(parent) =>
        row(lang.childLabel, parent.abbrev)
    }

  private def fmtAssessmentMethod(
      label: String,
      ams: ModuleAssessmentMethods
  )(implicit lang: PrintingLanguage): Printer[Unit] =
    NonEmptyList
      .fromList(ams.mandatory)
      .fold(row(label, lang.noneLabel)) { xs =>
        rows(
          label,
          xs.map { am =>
            val methodValue = lang.value(am.method)
            am.percentage.fold(methodValue)(d =>
              s"$methodValue (${fmtDouble(d)} %)"
            )
          }
        )
      }

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

  private def workload(implicit
      m: Metadata,
      language: PrintingLanguage
  ) = {
    val (workload, contactHour, selfStudy) = language.workload(m.workload)
    row(workload._1, workload._2)
      .skip(row(contactHour._1, contactHour._2))
      .skip(row(selfStudy._1, selfStudy._2))
  }

  private def pos(studyProgram: String => Option[StudyProgramView])(implicit
      m: Metadata,
      language: PrintingLanguage
  ) =
    fmtPOs(language.poLabel, m.pos, studyProgram)

  private def lastModified(implicit
      lang: PrintingLanguage,
      localDateTime: LocalDateTime
  ) =
    prefix(
      s"${lang.lastModifiedLabel} ${localDateTime.format(localDatePattern)}"
    )

  private def header(implicit m: Metadata) =
    prefix("# ").skip(prefix(m.title))

  private def particularities(de: ModuleContent, en: ModuleContent)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.particularitiesLabel,
      de.particularities,
      en.particularities
    )

  private def recommendedReading(de: ModuleContent, en: ModuleContent)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.recommendedReadingLabel,
      de.recommendedReading,
      en.recommendedReading
    )

  private def teachingAndLearningMethods(de: ModuleContent, en: ModuleContent)(
      implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.teachingAndLearningMethodsLabel,
      de.teachingAndLearningMethods,
      en.teachingAndLearningMethods
    )

  private def moduleContent(de: ModuleContent, en: ModuleContent)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(lang.moduleContentLabel, de.content, en.content)

  private def learningOutcome(de: ModuleContent, en: ModuleContent)(implicit
      lang: PrintingLanguage,
      substituteLocalisedContent: Boolean
  ) =
    contentBlock(
      lang.learningOutcomeLabel,
      de.learningOutcome,
      en.learningOutcome
    )

  def printer(studyProgram: String => Option[StudyProgramView])(implicit
      lang: PrintingLanguage,
      localDateTime: Option[LocalDateTime]
  ): Printer[Module] =
    Printer { case (module, input) =>
      implicit val m: Metadata = module.metadata
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
        .skip(learningOutcome(module.deContent, module.enContent))
        .skip(moduleContent(module.deContent, module.enContent))
        .skip(teachingAndLearningMethods(module.deContent, module.enContent))
        .skip(recommendedReading(module.deContent, module.enContent))
        .skip(particularities(module.deContent, module.enContent))
        .skipOpt(
          localDateTime.map(d =>
            prefix("---").skip(newline.repeat(2)).skip(lastModified(lang, d))
          )
        )
        .print((), input)
    }
}
