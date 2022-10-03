package parsing.types

import basedata.StudyProgramWithPO

case class ParsedPOs(
    mandatory: List[POMandatory],
    optional: List[ParsedPOOptional]
)

case class ParsedPOOptional(
    studyProgram: StudyProgramWithPO,
    instanceOf: String,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class POMandatory(
    studyProgram: StudyProgramWithPO,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
