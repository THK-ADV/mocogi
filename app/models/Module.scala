package models

import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class Module(id: UUID, title: String, abbrev: String)

object Module {
  implicit def writes: Writes[Module] = Json.writes
}
