package models

import play.api.libs.json.{Format, Json}

case class SpecializationShort(abbrev: String, label: String)

object SpecializationShort {
  implicit def format: Format[SpecializationShort] = Json.format
}
