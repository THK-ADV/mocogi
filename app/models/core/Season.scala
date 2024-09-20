package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class Season(id: String, deLabel: String, enLabel: String) extends IDLabel

object Season {
  implicit def writes: Writes[Season] = Json.writes
}
