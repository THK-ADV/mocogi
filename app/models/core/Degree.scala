package models.core

import play.api.libs.json.Format
import play.api.libs.json.Json

case class Degree(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object Degree {
  implicit def ord: Ordering[Degree] = Ordering.by[Degree, String](_.id)

  given Format[Degree] = Json.format
}
