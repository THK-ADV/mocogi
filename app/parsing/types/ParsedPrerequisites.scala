package parsing.types

import basedata.StudyProgram

case class ParsedPrerequisites(
    recommended: Option[ParsedPrerequisiteEntry],
    required: Option[ParsedPrerequisiteEntry]
)

case class ParsedPrerequisiteEntry(
    text: String,
    modules: List[String],
    studyPrograms: List[StudyProgram]
)
