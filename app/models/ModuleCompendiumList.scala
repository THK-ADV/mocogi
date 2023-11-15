package models

import play.api.libs.json.{Json, Writes}

import java.time.LocalDateTime

case class ModuleCompendiumList[StudyProgram, Semester](
    poAbbrev: String,
    poNumber: Int,
    studyProgram: StudyProgram,
    semester: Semester,
    deUrl: String,
    enUrl: String,
    generated: LocalDateTime
)

object ModuleCompendiumList {
  type DB = ModuleCompendiumList[String, String]
  type Atomic = ModuleCompendiumList[StudyProgramShort, Semester]

  implicit def atomicWrites: Writes[Atomic] = Json.writes
}
