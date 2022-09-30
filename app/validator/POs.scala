package validator

import basedata.StudyProgram
import parsing.types.POMandatory

case class POs(
    mandatory: List[POMandatory],
    optional: List[POOptional]
)

case class POOptional(
    studyProgram: StudyProgram,
    instanceOf: Module,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
