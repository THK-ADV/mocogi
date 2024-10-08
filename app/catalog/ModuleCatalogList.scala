package catalog

import java.time.LocalDateTime

import models.StudyProgramView
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleCatalogList(
    studyProgram: StudyProgramView,
    semester: Semester,
    deUrl: String,
    enUrl: String,
    generated: LocalDateTime
)

object ModuleCatalogList {
  implicit def writes: Writes[ModuleCatalogList] = Json.writes
}
