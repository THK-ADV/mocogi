package models

import controllers.JsonNullWritable
import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModulePrerequisitesProtocol(
    recommended: Option[ModulePrerequisiteEntryProtocol],
    required: Option[ModulePrerequisiteEntryProtocol]
)

object ModulePrerequisitesProtocol extends JsonNullWritable {
  implicit def format: Format[ModulePrerequisitesProtocol] = Json.format
}
