package models

import play.api.libs.json.{Format, Json}

import java.util.UUID

case class ModulePrerequisiteEntryProtocol(
    text: String,
    modules: List[UUID],
    pos: List[String]
)

object ModulePrerequisiteEntryProtocol {
  implicit def format: Format[ModulePrerequisiteEntryProtocol] = Json.format
}
