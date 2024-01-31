package models.core

import play.api.libs.json.{Json, Writes}

case class Grade(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object Grade {
  implicit def ord: Ordering[Grade] = Ordering.by[Grade, String](_.id)

  implicit def writes: Writes[Grade] = Json.writes
}
