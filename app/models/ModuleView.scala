package models

import models.core.IDLabel

import java.util.UUID

case class ModuleView[ModuleManagement, StudyProgram](
    id: UUID,
    title: String,
    abbrev: String,
    ects: Double,
    moduleManagement: ModuleManagement,
    studyProgram: StudyProgram
)

case class StudyProgramModuleAssociation[Semester](
    poId: String,
    studyProgramId: String,
    studyProgramLabel: String,
    degreeLabel: String,
    version: Int,
    specialization: Option[IDLabel],
    mandatory: Boolean,
    recommendedSemester: Semester
)

case class ModuleManagement(
    id: String,
    abbreviation: String,
    kind: String,
    title: String,
    firstname: String,
    lastname: String
)
