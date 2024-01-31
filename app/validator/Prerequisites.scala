package validator

import models.Module
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
    modules: List[Module],
    pos: List[PO]
)

object PrerequisiteEntry {
  implicit def writes: Writes[PrerequisiteEntry] = Json.writes
}
