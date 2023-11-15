package models

import database.view.SpecializationShort

case class POShort(
    abbrev: String,
    version: Int,
    specialization: Option[SpecializationShort],
    studyProgram: StudyProgramShort
)
