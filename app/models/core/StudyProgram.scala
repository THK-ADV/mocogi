package models.core

import java.time.LocalDate

case class StudyProgram(
    abbrev: String,
    deLabel: String,
    enLabel: String,
    internalAbbreviation: String,
    externalAbbreviation: String,
    deUrl: String,
    enUrl: String,
    grade: Grade,
    programDirector: Person,
    accreditationUntil: LocalDate,
    studyForm: List[StudyForm],
    language: List[Language],
    seasons: List[Season],
    campus: List[Location],
    restrictedAdmission: RestrictedAdmission,
    deDescription: String,
    deNote: String,
    enDescription: String,
    enNote: String
)

case class StudyForm(
    kind: StudyFormType,
    workloadPerEcts: Int,
    scope: List[StudyFormScope]
)

case class StudyFormScope(
    programDuration: Int,
    totalEcts: Int,
    deReason: String,
    enReason: String
)

case class RestrictedAdmission(
    value: Boolean,
    deReason: String,
    enReason: String
)
