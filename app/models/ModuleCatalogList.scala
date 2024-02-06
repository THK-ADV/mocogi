package models

import play.api.libs.json.{Json, Writes}

import java.time.LocalDateTime

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
