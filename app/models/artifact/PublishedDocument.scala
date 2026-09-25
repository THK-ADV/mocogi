package models.artifact

import java.time.LocalDate

import models.Semester
import models.StudyProgramView
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class PublishedDocument(studyProgram: StudyProgramView, semester: Semester, date: LocalDate, url: String)

object PublishedDocument {
  given Writes[PublishedDocument] = Json.writes
}
