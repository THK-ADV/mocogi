package validator

import basedata.PO
import parsing.types.POMandatory

case class POs(
    mandatory: List[POMandatory],
    optional: List[POOptional]
)

case class POOptional(
    studyProgram: PO,
    instanceOf: Module,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
