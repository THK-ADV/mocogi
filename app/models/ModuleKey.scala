package models

import models.core.AbbrevLabelDescLike
import play.api.libs.json.{Json, Writes}

case class ModuleKey(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends AbbrevLabelDescLike

object ModuleKey {
  implicit def writes: Writes[ModuleKey] = Json.writes
}
