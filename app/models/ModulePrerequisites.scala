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

  def toProtocol(p: ModulePrerequisites): ModulePrerequisitesProtocol =
    ModulePrerequisitesProtocol(
      p.recommended.map(ModulePrerequisiteEntry.toProtocol),
      p.required.map(ModulePrerequisiteEntry.toProtocol)
    )
}

case class ModulePrerequisiteEntry(
    text: String,
    modules: List[ModuleCore],
    @Deprecated(forRemoval = true) pos: List[PO]
)

object ModulePrerequisiteEntry {
  implicit def writes: Writes[ModulePrerequisiteEntry] = Json.writes

  def toProtocol(e: ModulePrerequisiteEntry): ModulePrerequisiteEntryProtocol =
    ModulePrerequisiteEntryProtocol(e.text, e.modules.map(_.id), e.pos.map(_.id))
}
