package parsing.types

case class Prerequisites(
    text: String,
    modules: List[String],
    studyPrograms: List[String]
)
