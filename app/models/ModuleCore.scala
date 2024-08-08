package models

import play.api.libs.json.{Json, OWrites}

import java.util.UUID

case class ModuleCore(id: UUID, title: String, abbrev: String)

object ModuleCore {
  implicit def writes: OWrites[ModuleCore] = Json.writes
}
