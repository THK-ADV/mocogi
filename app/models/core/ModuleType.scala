package models.core

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleType(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleType {
  given Format[ModuleType] = Json.format

  def genericId = "generic_module"

  def isGeneric(id: String): Boolean = id == genericId
}
