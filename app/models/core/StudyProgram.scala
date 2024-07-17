package models.core

import cats.data.NonEmptyList
import controllers.NelWrites
import play.api.libs.json.{Json, Writes}

case class StudyProgram(
    id: String,
    deLabel: String,
    enLabel: String,
    degree: String,
    programDirectors: NonEmptyList[String],
    examDirectors: NonEmptyList[String]
)

object StudyProgram extends NelWrites {
  implicit def writes: Writes[StudyProgram] = Json.writes
}
