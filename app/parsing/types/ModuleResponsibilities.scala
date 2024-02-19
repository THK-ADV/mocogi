package parsing.types

import models.core.Identity
import play.api.libs.json.{Json, Writes}

case class ModuleResponsibilities(
    moduleManagement: List[Identity],
    lecturers: List[Identity]
)

object ModuleResponsibilities {
  implicit def writes: Writes[ModuleResponsibilities] = Json.writes
}
