package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleType(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleType {
  implicit def writes: Writes[ModuleType] = Json.writes

  def genericId = "generic_module"
}
