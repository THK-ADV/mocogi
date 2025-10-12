package models

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ExamList(studyProgram: StudyProgramView, semester: Semester, date: LocalDate, url: String)

object ExamList {
  given Writes[ExamList] = Json.writes
}
