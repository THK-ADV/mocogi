package parsing.types

import cats.data.NonEmptyList
import controllers.NelWrites
import models.core.Identity
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleResponsibilities(
    moduleManagement: NonEmptyList[Identity],
    lecturers: NonEmptyList[Identity]
)

object ModuleResponsibilities extends NelWrites {
  implicit def writes: Writes[ModuleResponsibilities] = Json.writes
}
