package parsing.types

import basedata.StudyProgram

case class Prerequisites(
    recommended: Option[PrerequisiteEntry],
    required: Option[PrerequisiteEntry]
)

case class PrerequisiteEntry(
    text: String,
    modules: List[String],
    studyPrograms: List[StudyProgram]
)
