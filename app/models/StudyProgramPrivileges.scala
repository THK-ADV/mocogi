package models

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class StudyProgramPrivileges(
    studyProgram: StudyProgramView,
    canCreate: Boolean,
    canPreview: Boolean
)

object StudyProgramPrivileges {
  implicit def writes: Writes[StudyProgramPrivileges] = Json.writes
}
