package parsing.types

import basedata.StudyProgram

case class PrerequisiteEntry(
    text: String,
    modules: List[String],
    studyPrograms: List[StudyProgram]
)
