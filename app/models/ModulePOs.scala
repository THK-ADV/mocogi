package models

import models.core.{PO, Specialization}
import parsing.types.ModulePOMandatory
import play.api.libs.json.{Json, Writes}

case class ModulePOs(
    mandatory: List[ModulePOMandatory],
    optional: List[ModulePOOptional]
)

object ModulePOs {
  implicit def writes: Writes[ModulePOs] = Json.writes
}

case class ModulePOOptional(
    po: PO,
    specialization: Option[Specialization],
    instanceOf: Option[ModuleCore],
    partOfCatalog: Boolean,
    isFocus: Boolean,
    recommendedSemester: List[Int]
)

object ModulePOOptional {
  implicit def writes: Writes[ModulePOOptional] = Json.writes
}
