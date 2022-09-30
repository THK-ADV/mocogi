package validator

import parsing.types.StudyProgram

case class ValidPrerequisites(
    recommended: Option[ValidPrerequisiteEntry],
    required: Option[ValidPrerequisiteEntry]
)

case class ValidPrerequisiteEntry(
    text: String,
    modules: List[Module],
    studyPrograms: List[StudyProgram]
)
