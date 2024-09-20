package models

import java.util.UUID

import play.api.libs.json.Json
import play.api.libs.json.OWrites

case class ModuleCore(id: UUID, title: String, abbrev: String)

object ModuleCore {
  implicit def writes: OWrites[ModuleCore] = Json.writes
}
