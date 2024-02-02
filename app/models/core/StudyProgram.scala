package models.core

import play.api.libs.json.{Json, Writes}

case class StudyProgram(
    id: String,
    deLabel: String,
    enLabel: String,
    internalAbbreviation: String,
    externalAbbreviation: String,
    degree: String,
    programDirectors: List[String],
    examDirectors: List[String]
)

object StudyProgram {
  implicit def writes: Writes[StudyProgram] = Json.writes
}
