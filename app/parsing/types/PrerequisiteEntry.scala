package parsing.types

case class PrerequisiteEntry(
    text: String,
    modules: List[String],
    studyPrograms: List[String]
)
