package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleLanguage(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleLanguage {
  implicit def writes: Writes[ModuleLanguage] = Json.writes
}
