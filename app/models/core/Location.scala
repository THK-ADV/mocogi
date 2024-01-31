package models.core

import play.api.libs.json.{Json, Writes}

case class Location(id: String, deLabel: String, enLabel: String)
    extends IDLabel

object Location {
  implicit def writes: Writes[Location] = Json.writes
}
