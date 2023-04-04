package parsing.types

import models.core.{PO, Specialization}

import java.util.UUID

case class ParsedPOs(
    mandatory: List[POMandatory],
    optional: List[ParsedPOOptional]
)

case class ParsedPOOptional(
    po: PO,
    specialization: Option[Specialization],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class POMandatory(
    po: PO,
    specialization: Option[Specialization],
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
