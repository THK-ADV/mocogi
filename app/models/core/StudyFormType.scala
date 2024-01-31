package models.core

import play.api.libs.json.{Json, Writes}

case class StudyFormType(id: String, deLabel: String, enLabel: String)
    extends IDLabel

object StudyFormType {
  implicit def writes: Writes[StudyFormType] = Json.writes
}
