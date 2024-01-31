package models

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
    gradeLabel: String,
    version: Int,
    specialization: Option[SpecializationShort],
    mandatory: Boolean,
    recommendedSemester: Semester
)

case class ModuleManagement(
    id: String,
    abbrev: String,
    kind: String,
    title: String,
    firstname: String,
    lastname: String
)
