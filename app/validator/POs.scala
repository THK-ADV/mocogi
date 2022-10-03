package validator

import basedata.StudyProgramWithPO
import parsing.types.POMandatory

case class POs(
    mandatory: List[POMandatory],
    optional: List[POOptional]
)

case class POOptional(
    studyProgram: StudyProgramWithPO,
    instanceOf: Module,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
