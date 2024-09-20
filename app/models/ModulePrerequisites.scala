package models

import models.core.PO
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModulePrerequisites(
    recommended: Option[ModulePrerequisiteEntry],
    required: Option[ModulePrerequisiteEntry]
)

object ModulePrerequisites {
  implicit def writes: Writes[ModulePrerequisites] = Json.writes
}

case class ModulePrerequisiteEntry(
    text: String,
    modules: List[ModuleCore],
    pos: List[PO]
)

object ModulePrerequisiteEntry {
  implicit def writes: Writes[ModulePrerequisiteEntry] = Json.writes
}
