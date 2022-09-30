package parsing.types

import basedata.StudyProgram

case class POOptional(
    studyProgram: StudyProgram,
    instanceOf: String,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
