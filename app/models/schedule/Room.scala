package models.schedule

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json

case class Room(id: UUID, label: String, abbrev: String, `type`: String, capacity: Int)

object Room {
  given Format[Room] = Json.format
}
