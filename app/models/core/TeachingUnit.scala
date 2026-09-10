package models.core

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json

case class TeachingUnit(id: UUID, label: String, abbrev: String, faculty: String) {
  def isINF = abbrev == "INF"
  def isING = abbrev == "ING"
}

object TeachingUnit {
  given Format[TeachingUnit] = Json.format
}
