package validator

import models.core.PO
import parsing.types.POMandatory

case class POs(
    mandatory: List[POMandatory],
    optional: List[POOptional]
)

case class POOptional(
    po: PO,
    instanceOf: Module,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
