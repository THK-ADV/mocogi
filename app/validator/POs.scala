package validator

import models.ModuleCore
import models.core.{PO, Specialization}
import parsing.types.POMandatory
import play.api.libs.json.{Json, Writes}

case class POs(
    mandatory: List[POMandatory],
    optional: List[POOptional]
)

object POs {
  implicit def writes: Writes[POs] = Json.writes
}

case class POOptional(
    po: PO,
    specialization: Option[Specialization],
    instanceOf: ModuleCore,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

object POOptional {
  implicit def writes: Writes[POOptional] = Json.writes
}
