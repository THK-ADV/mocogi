package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleStatus(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleStatus {
  implicit def writes: Writes[ModuleStatus] = Json.writes

  def activeId = "active"

  def isActive(id: String) = id == activeId
}
