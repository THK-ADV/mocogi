package parsing.types

import models.core.Identity
import play.api.libs.json.{Json, Writes}

case class Responsibilities(
    moduleManagement: List[Identity],
    lecturers: List[Identity]
)

object Responsibilities {
  implicit def writes: Writes[Responsibilities] = Json.writes
}
