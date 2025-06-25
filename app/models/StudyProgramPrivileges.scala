package models

import play.api.libs.json.Json
import play.api.libs.json.Writes

@deprecated
case class StudyProgramPrivileges(
    studyProgram: StudyProgramView,
    roles: Set[UniversityRole]
)

object StudyProgramPrivileges {
  implicit def writes: Writes[StudyProgramPrivileges] = Json.writes
}
