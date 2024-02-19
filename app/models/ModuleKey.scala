package models

import models.core.IDLabelDesc
import play.api.libs.json.{Json, Writes}

case class ModuleKey(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc

object ModuleKey {
  implicit def writes: Writes[ModuleKey] = Json.writes
}
