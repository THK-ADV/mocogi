package parsing.types

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleParticipants(min: Int, max: Int)

object ModuleParticipants {
  given Format[ModuleParticipants] = Json.format
}
