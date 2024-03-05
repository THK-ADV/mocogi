package parsing.types

import models.core.{PO, Specialization}
import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class ParsedPOs(
    mandatory: List[ModulePOMandatory],
    optional: List[ParsedPOOptional]
)

case class ParsedPOOptional(
    po: PO,
    specialization: Option[Specialization],
    instanceOf: Option[UUID],
    partOfCatalog: Boolean,
    isFocus: Boolean,
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
