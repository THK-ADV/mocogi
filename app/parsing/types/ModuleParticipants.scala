package parsing.types

import play.api.libs.json.{Format, Json}

case class ModuleParticipants(min: Int, max: Int)

object ModuleParticipants {
  implicit def format: Format[ModuleParticipants] = Json.format
}
