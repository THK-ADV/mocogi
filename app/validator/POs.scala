package validator

import models.core.{PO, Specialization}
import parsing.types.POMandatory

case class POs(
    mandatory: List[POMandatory],
    optional: List[POOptional]
)

case class POOptional(
    po: PO,
    specialization: Option[Specialization],
    instanceOf: Module,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
