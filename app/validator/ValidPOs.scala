package validator

import parsing.types.{POMandatory, StudyProgram}

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
