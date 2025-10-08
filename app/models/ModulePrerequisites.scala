package models

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModulePrerequisites(
    recommended: Option[ModulePrerequisiteEntry],
    required: Option[ModulePrerequisiteEntry]
)

object ModulePrerequisites {
  implicit def writes: Writes[ModulePrerequisites] = Json.writes

  def toProtocol(p: ModulePrerequisites): ModulePrerequisitesProtocol =
    ModulePrerequisitesProtocol(
      p.recommended.map(ModulePrerequisiteEntry.toProtocol),
      p.required.map(ModulePrerequisiteEntry.toProtocol)
    )
}

case class ModulePrerequisiteEntry(text: String, modules: List[ModuleCore])

object ModulePrerequisiteEntry {
  implicit def writes: Writes[ModulePrerequisiteEntry] = Json.writes

  def toProtocol(e: ModulePrerequisiteEntry): ModulePrerequisiteEntryProtocol =
    ModulePrerequisiteEntryProtocol(e.text, e.modules.map(_.id))
}
