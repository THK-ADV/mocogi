package printing.markdown

import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.collection.mutable.ListBuffer

import cats.data.NonEmptyList
import models.*
import models.core.Identity
import parsing.types.*
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import printer.Printer
import printer.Printer.newline
import printer.Printer.prefix
import printing.*

@Singleton
@deprecated(message = "we will move to explicit rendered modules instead of static html files")
final class ModuleMarkdownPrinter @Inject() (
    private val message: MessagesApi,
    @Named("substituteLocalisedContent") private val substituteLocalisedContent: Boolean
) {

  private val strings = new LocalizedStrings(message)(using Lang(Locale.GERMANY))

  private def row(key: String, value: String): Printer[Unit] =
    prefix(s"| $key | $value |").skip(newline)

  private def rows(key: String, value: NonEmptyList[String]): Printer[Unit] =
    value.zipWithIndex
      .map {
        case (s, i) =>
          row(if (i == 0) key else "", s)
      }
      .reduceLeft(_ skip _)

  private def fmtPeople(label: String, xs: NonEmptyList[Identity]): Printer[Unit] = {
    rows(label, xs.map(fmtIdentity))
  }

  private def fmtPrerequisites(label: String, entry: Option[ModulePrerequisiteEntry]): Printer[Unit] =
    entry match {
      case Some(p) if p.text.nonEmpty || p.modules.nonEmpty =>
        val builder = new ListBuffer[String]()
        if p.text.nonEmpty then {
          builder.append(p.text)
        }
        if p.modules.nonEmpty then {
          val subBuilder  = new StringBuilder()
          val moduleLabel = strings.prerequisitesModuleLabel
          subBuilder.append(s"$moduleLabel: ")
          subBuilder.append(p.modules.map(a => s"${a.title} (${a.abbrev})").mkString(", "))
          builder.append(subBuilder.toString())
        }
        rows(label, NonEmptyList.fromList(builder.toList).get)
      case _ =>
        row(label, strings.noneLabel)
    }

  private def fmtPOs(label: String, pos: ModulePOs, studyProgram: String => Option[StudyProgramView]): Printer[Unit] = {
    def fmt(p: ModulePOMandatory) = {
      val semester = Option.when(p.recommendedSemester.nonEmpty)(
        s"(${strings.semesterLabel} ${fmtCommaSeparated(p.recommendedSemester)(_.toString)})"
      )
      val studyProgramWithPO = studyProgram(p.po.program) match {
        case Some(sp) =>
          val spLabel     = strings.label(sp)
          val degreeLabel = strings.label(sp.degree)
          s"$degreeLabel: $spLabel PO ${p.po.version}"
        case None =>
          p.po.id
      }
      semester.fold(studyProgramWithPO)(s => s"$studyProgramWithPO $s")
    }
    NonEmptyList
      .fromList(pos.mandatory)
      .fold(row(label, strings.noneLabel))(xs => rows(label, xs.map(fmt)))
  }

  private def fmtModuleRelation(relation: ModuleRelation): Printer[Unit] =
    relation match {
      case ModuleRelation.Parent(children) =>
        row(
          strings.parentLabel,
          fmtCommaSeparated(children.toList)(_.abbrev)
        )
      case ModuleRelation.Child(parent) =>
        row(strings.childLabel, parent.abbrev)
    }

  private def fmtAssessmentMethod(label: String, ams: ModuleAssessmentMethods): Printer[Unit] =
    NonEmptyList
      .fromList(ams.mandatory)
      .fold(row(label, strings.noneLabel)) { xs =>
        rows(
          label,
          xs.map { am =>
            val methodValue = strings.label(am.method)
            am.percentage.fold(methodValue)(d => s"$methodValue (${fmtDouble(d)} %)")
          }
        )
      }

  private def header(title: String) =
    prefix(s"## $title").skip(newline)

  def contentBlock(title: String, de: String, en: String) = {
    val content = {
      val l = if strings.isGerman then de else en
      if (l.isEmpty && substituteLocalisedContent) if strings.isGerman then en else de else l
    }
    val contentPrinter =
      if (content.isEmpty) newline
      else
        newline
          .skip(prefix(content))
          .skip(newline.repeat(2))
    header(title).skip(contentPrinter)
  }

  private def moduleNumber(implicit m: Metadata) =
    row(strings.moduleAbbrevLabel, m.abbrev)

  private def moduleTitle(implicit m: Metadata) =
    row(strings.moduleTitleLabel, m.title)

  private def moduleType(implicit m: Metadata) =
    row(strings.moduleTypeLabel, strings.label(m.kind))

  private def ects(implicit m: Metadata) =
    row(strings.ectsLabel, fmtDouble(m.ects.value))

  private def language(implicit m: Metadata) =
    row(strings.languageLabel, strings.label(m.language))

  private def duration(implicit m: Metadata) =
    row(strings.durationLabel, s"${m.duration} ${strings.semesterLabel}")

  private def frequency(implicit m: Metadata) =
    row(strings.frequencyLabel, strings.frequencyLabel(m.season))

  private def moduleCoordinator(implicit m: Metadata) =
    fmtPeople(strings.moduleCoordinatorLabel, m.responsibilities.moduleManagement)

  private def moduleLecturer(implicit m: Metadata) =
    fmtPeople(strings.lecturersLabel, m.responsibilities.lecturers)

  private def assessmentMethods(implicit m: Metadata) =
    fmtAssessmentMethod(strings.assessmentMethodLabel, m.assessmentMethods)

  private def recommendedPrerequisites(implicit m: Metadata) =
    fmtPrerequisites(strings.recommendedPrerequisitesLabel, m.prerequisites.recommended)

  private def requiredPrerequisites(implicit m: Metadata) =
    fmtPrerequisites(strings.requiredPrerequisitesLabel, m.prerequisites.required)

  private def workload(implicit m: Metadata) = {
    val (workload, contactHour, selfStudy) = strings.workloadLabels(m.workload, m.ects.value, 30)
    row(workload._1, workload._2)
      .skip(row(contactHour._1, contactHour._2))
      .skip(row(selfStudy._1, selfStudy._2))
  }

  private def pos(studyProgram: String => Option[StudyProgramView])(implicit m: Metadata) =
    fmtPOs(strings.poLabel, m.pos, studyProgram)

  private def lastModified(implicit localDateTime: LocalDateTime) =
    prefix(s"${strings.lastModifiedLabel} ${localDateTime.format(localDatePattern)}")

  private def header(implicit m: Metadata) =
    prefix("# ").skip(prefix(m.title))

  private def particularities(de: ModuleContent, en: ModuleContent) =
    contentBlock(strings.particularitiesMarkdownLabel, de.particularities, en.particularities)

  private def recommendedReading(de: ModuleContent, en: ModuleContent) =
    contentBlock(strings.recommendedReadingMarkdownLabel, de.recommendedReading, en.recommendedReading)

  private def teachingAndLearningMethods(de: ModuleContent, en: ModuleContent) =
    contentBlock(
      strings.teachingAndLearningMethodsMarkdownLabel,
      de.teachingAndLearningMethods,
      en.teachingAndLearningMethods
    )

  private def moduleContent(de: ModuleContent, en: ModuleContent) =
    contentBlock(strings.moduleContentMarkdownLabel, de.content, en.content)

  private def learningOutcome(de: ModuleContent, en: ModuleContent) =
    contentBlock(strings.learningOutcomeMarkdownLabel, de.learningOutcome, en.learningOutcome)

  def printer(studyProgram: String => Option[StudyProgramView])(implicit lastModified: LocalDateTime): Printer[Module] =
    Printer {
      case (module, input) =>
        implicit val m: Metadata = module.metadata

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
          .skip(prefix("---").skip(newline.repeat(2)).skip(this.lastModified(lastModified)))
          .print((), input)
    }
}
