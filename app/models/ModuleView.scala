package models

import java.util.UUID

case class ModuleView[ModuleManagement, StudyProgram](
    id: UUID,
    title: String,
    abbrev: String,
    ects: Double,
    status: String,
    moduleManagement: ModuleManagement,
    studyProgram: StudyProgram
)

case class StudyProgramModuleAssociation[Semester](
    studyProgram: StudyProgramView,
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
