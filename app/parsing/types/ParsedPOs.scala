package parsing.types

import basedata.PO

case class ParsedPOs(
    mandatory: List[POMandatory],
    optional: List[ParsedPOOptional]
)

case class ParsedPOOptional(
    studyProgram: PO,
    instanceOf: String,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class POMandatory(
    studyProgram: PO,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
