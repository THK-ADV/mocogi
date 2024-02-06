package validator

import models.ModuleCore
import models.core.PO
import play.api.libs.json.{Json, Writes}

case class Prerequisites(
    recommended: Option[PrerequisiteEntry],
    required: Option[PrerequisiteEntry]
)

object Prerequisites {
  implicit def writes: Writes[Prerequisites] = Json.writes
}

case class PrerequisiteEntry(
    text: String,
    modules: List[ModuleCore],
    pos: List[PO]
)

object PrerequisiteEntry {
  implicit def writes: Writes[PrerequisiteEntry] = Json.writes
}
