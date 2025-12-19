package parsing.types

import cats.data.NonEmptyList
import controllers.json.NelWrites
import models.core.Identity
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleResponsibilities(
    moduleManagement: NonEmptyList[Identity],
    lecturers: NonEmptyList[Identity]
)

object ModuleResponsibilities extends NelWrites {
  given Writes[ModuleResponsibilities] = Json.writes
}
