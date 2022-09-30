package validator

import basedata.StudyProgram
import parsing.types.POMandatory

case class ValidPOs(
    mandatory: List[POMandatory],
    optional: List[ValidPOOptional]
)

case class ValidPOOptional(
    studyProgram: StudyProgram,
    instanceOf: Module,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
