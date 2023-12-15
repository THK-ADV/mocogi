package models

import play.api.libs.json.{Json, Writes}

case class StudyProgramDirector(
    person: String,
    role: UniversityRole,
    studyProgram: StudyProgramShort,
    pos: Seq[PoSpec]
)

object StudyProgramDirector {
  implicit def writes: Writes[StudyProgramDirector] = Json.writes
}
