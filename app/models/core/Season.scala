package models.core

import play.api.libs.json.Format
import play.api.libs.json.Json

case class Season(id: String, deLabel: String, enLabel: String) extends IDLabel

object Season {
  given Format[Season] = Json.format
}
