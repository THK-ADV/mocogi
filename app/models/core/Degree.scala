package models.core

import play.api.libs.json.{Json, Writes}

case class Degree(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object Degree {
  implicit def ord: Ordering[Degree] = Ordering.by[Degree, String](_.id)

  implicit def writes: Writes[Degree] = Json.writes
}
