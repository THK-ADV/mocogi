package models.core

import play.api.libs.json.{Json, Writes}

case class Faculty(id: String, deLabel: String, enLabel: String) extends IDLabel

object Faculty {
  implicit def writes: Writes[Faculty] =
    Json.writes
}
