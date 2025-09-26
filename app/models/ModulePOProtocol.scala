package models

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModulePOProtocol(
    mandatory: List[ModulePOMandatoryProtocol],
    optional: List[ModulePOOptionalProtocol]
) {
  def hasPORelation(po: String) = mandatory.exists(_.po == po) || optional.exists(_.po == po)
}

object ModulePOProtocol {
  implicit def format: Format[ModulePOProtocol] = Json.format
}
