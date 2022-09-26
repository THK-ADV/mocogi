package parsing.types

case class POOptional(
    studyProgram: String,
    instanceOf: String,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)
