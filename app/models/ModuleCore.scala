package models

import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class ModuleCore(id: UUID, title: String, abbrev: String)

object ModuleCore {
  implicit def writes: Writes[ModuleCore] = Json.writes
}
