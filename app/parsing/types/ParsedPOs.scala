package parsing.types

import basedata.StudyProgram

case class ParsedPOs(
    mandatory: List[POMandatory],
    optional: List[ParsedPOOptional]
)

case class ParsedPOOptional(
    studyProgram: StudyProgram,
    instanceOf: String,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class POMandatory(
    studyProgram: StudyProgram,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
