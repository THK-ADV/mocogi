package validator

import parsing.types.StudyProgram

case class ValidPrerequisiteEntry (
  text: String,
  modules: List[Module],
  studyPrograms: List[StudyProgram]
)
