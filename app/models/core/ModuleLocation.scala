package models.core

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleLocation(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleLocation {
  given Format[ModuleLocation] = Json.format
}
