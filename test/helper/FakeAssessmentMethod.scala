package helper

import basedata.AssessmentMethod

trait FakeAssessmentMethod {
  implicit def fakeAssessmentMethod: Seq[AssessmentMethod] = Seq(
    AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
    AssessmentMethod(
      "written-exam-answer-choice-method",
      "Schriftliche Prüfungen im Antwortwahlverfahren",
      "--"
    ),
    AssessmentMethod("oral-exams", "Mündliche Prüfungen", "--"),
    AssessmentMethod("presentation", "Präsentation", "--"),
    AssessmentMethod("home-assignment", "Hausarbeit", "--"),
    AssessmentMethod("project", "Projektarbeit", "--"),
    AssessmentMethod(
      "project-documentation",
      "Projektdokumentation",
      "--"
    ),
    AssessmentMethod("portfolio", "Lernportfolio", "--"),
    AssessmentMethod("practical-report", "Praktikumsbericht", "--"),
    AssessmentMethod(
      "practical-semester-report",
      "Praxissemesterbericht",
      "--"
    ),
    AssessmentMethod("practical", "Praktikum", "--"),
    AssessmentMethod("test", "Schriftlicher Test", "--"),
    AssessmentMethod("thesis", "Schriftliche Ausarbeitung", "--")
  )
}
