package parsing.types

import java.util.UUID

import models.core.PO
import models.core.Specialization
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ParsedPOs(
    mandatory: List[ModulePOMandatory],
    optional: List[ParsedPOOptional]
)

case class ParsedPOOptional(
    po: PO,
    specialization: Option[Specialization],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class ModulePOMandatory(
    po: PO,
    specialization: Option[Specialization],
    recommendedSemester: List[Int]
)

object ModulePOMandatory {
  implicit def writes: Writes[ModulePOMandatory] = Json.writes
}
