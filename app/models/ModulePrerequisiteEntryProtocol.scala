package models

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModulePrerequisiteEntryProtocol(
    text: String,
    modules: List[UUID],
    @Deprecated pos: List[String]
)

object ModulePrerequisiteEntryProtocol {
  implicit def format: Format[ModulePrerequisiteEntryProtocol] = Json.format
}
