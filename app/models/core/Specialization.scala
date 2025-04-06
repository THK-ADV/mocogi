package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class Specialization(id: String, label: String, abbreviation: String, po: String)

object Specialization {
  implicit def writes: Writes[Specialization] = Json.writes
}
