package validator

import basedata.StudyProgramWithPO

case class Prerequisites(
    recommended: Option[PrerequisiteEntry],
    required: Option[PrerequisiteEntry]
)

case class PrerequisiteEntry(
    text: String,
    modules: List[Module],
    studyPrograms: List[StudyProgramWithPO]
)
