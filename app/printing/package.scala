import models.core._
import validator.Workload

import java.time.format.DateTimeFormatter
import scala.annotation.unused

package object printing {
  def localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

  def fmtDouble(d: Double): String =
    if (d % 1 == 0) d.toInt.toString
    else d.toString.replace('.', ',')

  def fmtIdentity(p: Identity): String =
    p match {
      case s: Identity.Person =>
        s"${s.title} ${s.fullName} (${fmtCommaSeparated(s.faculties, ", ")(_.id.toUpperCase)})"
      case g: Identity.Group =>
        g.label
      case u: Identity.Unknown =>
        u.label
    }

  def fmtCommaSeparated[A](xs: Seq[A], sep: String = ", ")(
      f: A => String
  ): String = {
    val builder = new StringBuilder()
    xs.zipWithIndex.foreach { case (a, i) =>
      builder.append(f(a))
      if (i < xs.size - 1)
        builder.append(sep)
    }
    builder.toString()
  }

  final implicit class LanguageOps(private val self: PrintingLanguage)
      extends AnyVal {

    def moduleCompendiumHeadline = self.fold("Modulhandbuch", "Module Handbook")

    def prologHeadline = "Prolog"

    def previewLabel = self.fold("Vorschau", "Preview")

    def moduleHeadline = self.fold("Module", "Modules")

    def studyPlanHeadline = self.fold("Studienverlaufspläne", "Study Plan")

    def moduleCodeLabel = self.fold("Modulnummer", "Module Code")

    def moduleTitleLabel = self.fold("Modulbezeichnung", "Module Title")

    def moduleTypeLabel = self.fold("Art des Moduls", "Type of Module")

    def ectsLabel = "ECTS credits"

    def languageLabel = self.fold("Sprache", "Language")

    def durationLabel = self.fold("Dauer des Moduls", "Duration of Module")

    @unused
    def recommendedSemesterLabel =
      self.fold("Empfohlenes Studiensemester", "Recommended for Semester")

    def frequencyLabel = self.fold("Häufigkeit des Angebots", "Frequency")

    def moduleCoordinatorLabel =
      self.fold("Modulverantwortliche*r", "Module Coordinator")

    def lecturersLabel = self.fold("Dozierende", "Lecturers")

    def assessmentMethodLabel = self.fold("Prüfungsformen", "Assessment Method")

    def workloadLabel = "Workload"

    def contactHoursLabel = self.fold("Präsenzzeit", "Contact hours")

    def selfStudyLabel = self.fold("Selbststudium", "Self-study")

    def recommendedPrerequisitesLabel =
      self.fold("Empfohlene Voraussetzungen", "Recommended Prerequisites")

    def requiredPrerequisitesLabel =
      self.fold("Zwingende Voraussetzungen", "Required Prerequisites")

    def poLabel = self.fold(
      "Verwendung des Moduls in weiteren Studiengängen",
      "Use of the Module in Other Degree Programs"
    )

    def poLabelShort = self.fold(
      "In anderen Studiengängen",
      "Used in other Programs"
    )

    def lastModifiedLabel =
      self.fold("Letzte Aktualisierung am", "Last update at")

    def parentLabel =
      self.fold("Besteht aus den Teilmodulen", "Consists of the submodules")

    def childLabel = self.fold("Gehört zum Modul", "Part of module")

    def noneLabel = self.fold("Keine", "None")

    def prerequisitesTextLabel = self.fold("Beschreibung", "Description")

    def prerequisitesModuleLabel = self.fold("Module", "Modules")

    def prerequisitesStudyProgramLabel =
      self.fold("Studiengänge", "Degree Programs")

    def semesterLabel = "Semester"

    def learningOutcomeLabel =
      self.fold("Angestrebte Lernergebnisse", "Learning Outcome")

    def moduleContentLabel = self.fold("Modulinhalte", "Module Content")

    def teachingAndLearningMethodsLabel = self.fold(
      "Lehr- und Lernmethoden (Medienformen)",
      "Teaching and Learning Methods"
    )

    def recommendedReadingLabel =
      self.fold("Empfohlene Literatur", "Recommended Reading")

    def particularitiesLabel = self.fold("Besonderheiten", "Particularities")

    def value(a: IDLabel): String =
      self.fold(a.deLabel, a.enLabel)

    def frequencyValue(season: Season): String =
      self.fold(s"Jedes ${season.deLabel}", s"Each ${season.enLabel}")

    def durationValue(duration: Int): String =
      s"$duration ${self.semesterLabel}"

    def workload(
        wl: Workload
    ): ((String, String), (String, String), (String, String)) = {
      val contactHoursValue = wl.total - wl.selfStudy
      val contactHoursParts = List(
        self.lectureValue(wl),
        self.exerciseValue(wl),
        self.practicalValue(wl),
        self.seminarValue(wl),
        self.projectSupervisionValue(wl),
        self.projectWorkValue(wl)
      ).filter(_.nonEmpty)
      val contactHoursParts0 = fmtCommaSeparated(contactHoursParts)(identity)
      val totalWlLabel = s"${wl.total} h"
      val contactHoursValueLabel =
        if (contactHoursParts0.isEmpty) s"$contactHoursValue h"
        else s"$contactHoursValue h ($contactHoursParts0)"
      val selfStudyLabel =
        if (wl.selfStudy == 0) self.noneLabel
        else s"${wl.selfStudy} h"

      (
        (self.workloadLabel, totalWlLabel),
        (self.contactHoursLabel, contactHoursValueLabel),
        (self.selfStudyLabel, selfStudyLabel)
      )
    }

    def lectureValue(wl: Workload): String =
      if (wl.lecture == 0) ""
      else {
        val value = s"${wl.lecture} h"
        val res = self.fold("Vorlesung", "Lecture")
        s"$value $res"
      }

    def exerciseValue(wl: Workload): String =
      if (wl.exercise == 0) ""
      else {
        val value = s"${wl.exercise} h"
        val res = self.fold("Übung", "Exercise")
        s"$value $res"
      }

    def practicalValue(wl: Workload): String =
      if (wl.practical == 0) ""
      else {
        val value = s"${wl.practical} h"
        val res = self.fold("Praktikum", "Practical")
        s"$value $res"
      }

    def seminarValue(wl: Workload): String =
      if (wl.seminar == 0) ""
      else {
        val value = s"${wl.seminar} h"
        val res = "Seminar"
        s"$value $res"
      }

    def projectSupervisionValue(wl: Workload): String =
      if (wl.projectSupervision == 0) ""
      else {
        val value = s"${wl.projectSupervision} h"
        val res = self.fold("Projektbetreuung", "Project Supervision")
        s"$value $res"
      }

    def projectWorkValue(wl: Workload): String =
      if (wl.projectWork == 0) ""
      else {
        val value = s"${wl.projectWork} h"
        val res = self.fold("Projektarbeit", "Project Work")
        s"$value $res"
      }
  }

  final implicit class LabelOps(private val self: Label) extends AnyVal {
    def localizedLabel(implicit lang: PrintingLanguage): String =
      lang.fold(self.deLabel, self.enLabel)

    def localizedLabel(
        specialization: Option[Label]
    )(implicit lang: PrintingLanguage): String =
      specialization.fold(self.localizedLabel)(s =>
        s"${self.localizedLabel} (${s.localizedLabel})"
      )
  }

  final implicit class IDLabelDescOps(
      private val self: IDLabelDesc
  ) extends AnyVal {
    def localizedDesc(implicit lang: PrintingLanguage): String =
      lang.fold(self.deDesc, self.enDesc)
  }
}
