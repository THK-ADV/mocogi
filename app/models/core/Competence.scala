package models.core

import play.api.libs.json.{Json, Writes}

case class Competence(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object Competence {
  implicit def writes: Writes[Competence] = Json.writes
}
