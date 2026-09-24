package models

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleCatalog(studyProgram: StudyProgramView, semester: Semester, date: LocalDate, url: String)

object ModuleCatalog {
  given Writes[ModuleCatalog] = Json.writes
}
