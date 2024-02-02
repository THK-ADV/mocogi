package models

import play.api.libs.json.{Json, Writes}

import java.time.LocalDateTime

case class ModuleCompendiumList(
    studyProgram: StudyProgramView,
    semester: Semester,
    deUrl: String,
    enUrl: String,
    generated: LocalDateTime
)

object ModuleCompendiumList {
  implicit def writes: Writes[ModuleCompendiumList] = Json.writes
}
