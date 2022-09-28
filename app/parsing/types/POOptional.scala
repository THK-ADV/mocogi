package parsing.types

case class POOptional(
    studyProgram: StudyProgram,
    instanceOf: String,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
