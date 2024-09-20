package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleCompetence(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object ModuleCompetence {
  implicit def writes: Writes[ModuleCompetence] = Json.writes
}
