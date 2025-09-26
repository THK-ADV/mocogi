package printing

import models.core.IDLabelDesc
import models.core.Label
import models.core.Season
import models.ModuleWorkload
import play.api.i18n.Lang
import play.api.i18n.MessagesApi

// TODO rename labels if needed
final class LocalizedStrings(messages: MessagesApi)(using lang: Lang) {

  val isGerman = lang.code.startsWith("de")

  def headline = messages("latex.module_catalog.headline")

  def previewLabel = messages("latex.module_catalog.preview_label")

  def languagePackage = messages("latex.lang.package_name")

  def moduleAbbrevLabel = messages("latex.module_catalog.module_abbrev")

  def noneLabel = messages("latex.module_catalog.none")

  def unknownLabel = messages("latex.module_catalog.unknown")

  def semesterLabel = messages("latex.module_catalog.semester")

  def moduleTitleLabel = messages("latex.module_catalog.module_title")

  def moduleTypeLabel = messages("latex.module_catalog.module_type")

  def ectsLabel = messages("latex.module_catalog.ects")

  def languageLabel = messages("latex.module_catalog.language")

  def durationLabel = messages("latex.module_catalog.duration")

  def recommendedSemesterLabel = messages("latex.module_catalog.recommended_semester")

  def frequencyLabel = messages("latex.module_catalog.frequency")

  def moduleCoordinatorLabel = messages("latex.module_catalog.module_management")

  def lecturersLabel = messages("latex.module_catalog.lecturer")

  def assessmentMethodLabel = messages("latex.module_catalog.assessment_method")

  def workloadLabel = messages("Workload")

  def contactHoursLabel = messages("latex.module_catalog.workload.contact_hours")

  def selfStudyLabel = messages("latex.module_catalog.workload.self_study")

  def recommendedPrerequisitesLabel = messages("latex.module_catalog.prerequisite.recommended")

  def requiredPrerequisitesLabel = messages("latex.module_catalog.prerequisite.required")

  def attendanceRequirementLabel = messages("latex.module_catalog.prerequisite.attendance")

  def assessmentPrerequisiteLabel = messages("latex.module_catalog.prerequisite.assessment")

  def poLabel = messages("latex.module_catalog.po")

  def poLabelShort = messages("latex.module_catalog.po_short")

  def lastModifiedLabel = messages("latex.module_catalog.last_modified")

  def parentLabel = messages("latex.module_catalog.parent")

  def childLabel = messages("latex.module_catalog.child")

  def prerequisitesModuleLabel = messages("latex.module_catalog.prerequisite.module")

  def learningOutcomeLabel = messages("latex.module_catalog.content.learning_outcome")

  def moduleContentLabel = messages("latex.module_catalog.content.module")

  def teachingAndLearningMethodsLabel = messages("latex.module_catalog.content.teaching_methods")

  def recommendedReadingLabel = messages("latex.module_catalog.content.reading")

  def particularitiesLabel = messages("latex.module_catalog.content.particularities")

  def frequencyLabel(season: Season): String =
    messages("latex.module_catalog.season", label(season))

  def label(l: Label): String =
    if isGerman then l.deLabel else l.enLabel

  def label(l: Option[Label]): String =
    l.fold("???")(label)

  def description(l: IDLabelDesc): String =
    if isGerman then l.deDesc else l.enDesc

  def label(l: Label, spec: Option[Label]): String =
    spec.fold(label(l))(s => s"${label(l)} (${label(s)})")

  def workloadLabels(
      wl: ModuleWorkload,
      ects: Double,
      ectsFactor: Int
  ): ((String, String), (String, String), (String, String)) = {
    def contactHoursValue(wl: ModuleWorkload, total: Int, selfStudy: Int) = {
      val nonEmptyParts = List(
        lectureValue(wl),
        exerciseValue(wl),
        practicalValue(wl),
        seminarValue(wl),
        projectSupervisionValue(wl),
        projectWorkValue(wl)
      ).filter(_.nonEmpty)
      val parts = fmtCommaSeparated(nonEmptyParts)(identity)

      val contactHours = total - selfStudy
      if (parts.isEmpty) s"$contactHours h"
      else s"$contactHours h ($parts)"
    }

    def selfStudyValue(selfStudy: Int) =
      if (selfStudy == 0) noneLabel else s"$selfStudy h"

    val total     = (ects * ectsFactor).toInt
    val selfStudy = wl.selfStudy(total)

    (
      (workloadLabel, s"$total h"),
      (contactHoursLabel, contactHoursValue(wl, total, selfStudy)),
      (selfStudyLabel, selfStudyValue(selfStudy))
    )
  }

  private def lectureValue(wl: ModuleWorkload): String =
    if (wl.lecture == 0) ""
    else messages("latex.module_catalog.workload.lecture", wl.lecture)

  private def exerciseValue(wl: ModuleWorkload): String =
    if (wl.exercise == 0) ""
    else messages("latex.module_catalog.workload.exercise", wl.exercise)

  private def practicalValue(wl: ModuleWorkload): String =
    if (wl.practical == 0) ""
    else messages("latex.module_catalog.workload.practical", wl.practical)

  private def seminarValue(wl: ModuleWorkload): String =
    if (wl.seminar == 0) ""
    else messages("latex.module_catalog.workload.seminar", wl.seminar)

  private def projectSupervisionValue(wl: ModuleWorkload): String =
    if (wl.projectSupervision == 0) ""
    else messages("latex.module_catalog.workload.projectSupervision", wl.projectSupervision)

  private def projectWorkValue(wl: ModuleWorkload): String =
    if (wl.projectWork == 0) ""
    else messages("latex.module_catalog.workload.projectWork", wl.projectWork)

  private def fmtCommaSeparated[A](xs: Seq[A], sep: String = ", ")(
      f: A => String
  ): String = {
    val builder = new StringBuilder()
    xs.zipWithIndex.foreach {
      case (a, i) =>
        builder.append(f(a))
        if (i < xs.size - 1)
          builder.append(sep)
    }
    builder.toString()
  }
}
