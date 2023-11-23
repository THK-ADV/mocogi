package models

import play.api.libs.json.{Json, Writes}

import java.time.LocalDateTime

case class ModuleCompendiumList[StudyProgram, Semester, Specialization](
    fullPo: String,
    poAbbrev: String,
    poNumber: Int,
    specialization: Option[Specialization],
    studyProgram: StudyProgram,
    semester: Semester,
    deUrl: String,
    enUrl: String,
    generated: LocalDateTime
)

object ModuleCompendiumList {
  type DB = ModuleCompendiumList[String, String, String]
  type Atomic =
    ModuleCompendiumList[StudyProgramShort, Semester, SpecializationShort]

  implicit def atomicWrites: Writes[Atomic] = Json.writes
}
