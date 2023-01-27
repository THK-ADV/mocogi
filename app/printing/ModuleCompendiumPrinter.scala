package printing

import basedata.{AbbrevLabelLike, Season}
import parsing.types._
import printer.Printer
import validator.Workload

import java.time.LocalDateTime

trait ModuleCompendiumPrinter {
  def printer(implicit
      language: PrintingLanguage,
      lastModified: LocalDateTime
  ): Printer[ModuleCompendium]
}

object ModuleCompendiumPrinter {
  implicit class LanguageOps(lang: PrintingLanguage) {
    def moduleCodeLabel = lang.fold("Modulnummer", "Module Code")
    def moduleTitleLabel = lang.fold("Modulbezeichnung", "Module Title")
    def moduleTypeLabel = lang.fold("Art des Moduls", "Type of Module")
    def ectsLabel = "ECTS credits"
    def languageLabel = lang.fold("Sprache", "Language")
    def durationLabel = lang.fold("Dauer des Moduls", "Duration of Module")
    def recommendedSemesterLabel =
      lang.fold("Empfohlenes Studiensemester", "Recommended for Semester")
    def frequencyLabel = lang.fold("Häufigkeit des Angebots", "Frequency")
    def moduleCoordinatorLabel =
      lang.fold("Modulverantwortliche*r", "Module Coordinator")
    def lecturersLabel = lang.fold("Dozierende", "Lecturers")
    def assessmentMethodLabel = lang.fold("Prüfungsformen", "Assessment Method")
    def workloadLabel = "Workload"
    def contactHoursLabel = lang.fold("Präsenzzeit", "Contact hours")
    def selfStudyLabel = lang.fold("Selbststudium", "Self-study")
    def recommendedPrerequisitesLabel =
      lang.fold("Empfohlene Voraussetzungen", "Recommended Prerequisites")
    def requiredPrerequisitesLabel =
      lang.fold("Zwingende Voraussetzungen", "Required Prerequisites")
    def poLabel = lang.fold(
      "Verwendung des Moduls in weiteren Studiengängen",
      "Use of the Module in Other Degree Programs"
    )
    def lastModifiedLabel =
      lang.fold("Letzte Aktualisierung am", "Last update at")
    def parentLabel =
      lang.fold("Besteht aus den Teilmodulen", "Consists of the submodules")
    def childLabel = lang.fold("Gehört zum Modul", "Part of module")
    def noneLabel = lang.fold("Keine", "None")
    def prerequisitesTextLabel = lang.fold("Beschreibung", "Description")
    def prerequisitesModuleLabel = lang.fold("Module", "Modules")
    def prerequisitesStudyProgramLabel =
      lang.fold("Studiengänge", "Degree Programs")
    def semesterLabel = "Semester"

    def learningOutcomeLabel =
      lang.fold("Angestrebte Lernergebnisse", "Learning Outcome")
    def moduleContentLabel = lang.fold("Modulinhalte", "Module Content")
    def teachingAndLearningMethodsLabel = lang.fold(
      "Lehr- und Lernmethoden (Medienformen)",
      "Teaching and Learning Methods"
    )
    def recommendedReadingLabel =
      lang.fold("Empfohlene Literatur", "Recommended Reading")
    def particularitiesLabel = lang.fold("Besonderheiten", "Particularities")

    def value(a: AbbrevLabelLike): String =
      lang.fold(a.deLabel, a.enLabel)

    def frequencyValue(season: Season): String =
      lang.fold(s"Jedes ${season.deLabel}", s"Each ${season.enLabel}")

    def lectureValue(wl: Workload): String =
      if (wl.lecture == 0) ""
      else {
        val value = s"${wl.lecture} h"
        val res = lang.fold("Vorlesung", "Lecture")
        s"$value $res"
      }

    def exerciseValue(wl: Workload): String =
      if (wl.exercise == 0) ""
      else {
        val value = s"${wl.exercise} h"
        val res = lang.fold("Übung", "Exercise")
        s"$value $res"
      }

    def practicalValue(wl: Workload): String =
      if (wl.practical == 0) ""
      else {
        val value = s"${wl.practical} h"
        val res = lang.fold("Praktikum", "Practical")
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
        val res = lang.fold("Projektbetreuung", "Project Supervision")
        s"$value $res"
      }

    def projectWorkValue(wl: Workload): String =
      if (wl.projectWork == 0) ""
      else {
        val value = s"${wl.projectWork} h"
        val res = lang.fold("Projektarbeit", "Project Work")
        s"$value $res"
      }
  }

  implicit class StringConcatOps(s: String) {
    def combine(other: String): String =
      if (other.isEmpty) s
      else s"$s, $other"
  }
}
