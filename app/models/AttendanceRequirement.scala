package models

import play.api.libs.json.Format
import play.api.libs.json.Json

case class AttendanceRequirement(min: String, reason: String, absence: String)

object AttendanceRequirement {
  given Format[AttendanceRequirement] = Json.format
}
