package models

import play.api.libs.json.{Json, Writes}

case class PoSpec(
    abbrev: String,
    version: Int,
    specialization: Option[SpecializationShort]
)

object PoSpec {
  implicit def writes: Writes[PoSpec] = Json.writes
}
