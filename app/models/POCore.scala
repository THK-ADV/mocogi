package models

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class POCore(id: String, version: Int)

object POCore {
  implicit def writes: Writes[POCore] = Json.writes
}
