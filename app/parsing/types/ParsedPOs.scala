package parsing.types

import basedata.PO

import java.util.UUID

case class ParsedPOs(
    mandatory: List[POMandatory],
    optional: List[ParsedPOOptional]
)

case class ParsedPOOptional(
    po: PO,
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class POMandatory(
    po: PO,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
