package models

import controllers.JsonNullWritable
import play.api.libs.json.{Format, Json}

case class ModulePrerequisitesProtocol(
    recommended: Option[ModulePrerequisiteEntryProtocol],
    required: Option[ModulePrerequisiteEntryProtocol]
)

object ModulePrerequisitesProtocol extends JsonNullWritable {
  implicit def format: Format[ModulePrerequisitesProtocol] = Json.format
}
