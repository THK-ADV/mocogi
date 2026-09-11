package models.core

import play.api.libs.json.Format
import play.api.libs.json.Json

case class Specialization(id: String, label: String, abbreviation: String, po: String)

object Specialization {
  given Format[Specialization] = Json.format
}
