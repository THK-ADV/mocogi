package models.core

import play.api.libs.json.{Json, Writes}

case class FocusArea(
    id: String,
    program: String,
    deLabel: String,
    enLabel: String,
    deDesc: String,
    enDesc: String
) extends IDLabelDesc

object FocusArea {
  implicit def writes: Writes[FocusArea] = Json.writes
}
