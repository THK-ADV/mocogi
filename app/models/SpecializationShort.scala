package models

import play.api.libs.json.{Format, Json}

case class SpecializationShort(id: String, label: String) // TODO remove?

object SpecializationShort {
  implicit def format: Format[SpecializationShort] = Json.format
}
