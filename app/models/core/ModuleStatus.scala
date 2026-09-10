package models.core

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleStatus(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleStatus {
  given Format[ModuleStatus] = Json.format

  def activeId = "active"

  def isActive(id: String) = id == activeId
}
