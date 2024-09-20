package models

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModulePOProtocol(
    mandatory: List[ModulePOMandatoryProtocol],
    optional: List[ModulePOOptionalProtocol]
)

object ModulePOProtocol {
  implicit def format: Format[ModulePOProtocol] = Json.format
}
