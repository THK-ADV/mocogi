package validator

import basedata.StudyProgram

case class Prerequisites(
    recommended: Option[PrerequisiteEntry],
    required: Option[PrerequisiteEntry]
)

case class PrerequisiteEntry(
    text: String,
    modules: List[Module],
    studyPrograms: List[StudyProgram]
)
