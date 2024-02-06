package models.core

import play.api.libs.json.{Json, Writes}

case class ModuleGlobalCriteria(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object ModuleGlobalCriteria {
  implicit def writes: Writes[ModuleGlobalCriteria] = Json.writes
}
