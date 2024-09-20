package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleLocation(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleLocation {
  implicit def writes: Writes[ModuleLocation] = Json.writes
}
