package models.core

import play.api.libs.json.{Json, Writes}

case class Status(id: String, deLabel: String, enLabel: String) extends IDLabel

object Status {
  implicit def writes: Writes[Status] = Json.writes
}
