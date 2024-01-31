package models.core

import play.api.libs.json.{Json, Writes}

case class GlobalCriteria(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object GlobalCriteria {
  implicit def writes: Writes[GlobalCriteria] = Json.writes
}
