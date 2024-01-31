package models.core

import play.api.libs.json.{Json, Writes}

import java.time.LocalDate

case class StudyProgram(
    id: String,
    deLabel: String,
    enLabel: String,
    internalAbbreviation: String,
    externalAbbreviation: String,
    deUrl: String,
    enUrl: String,
    grade: Grade,
    programDirectors: List[Identity],
    examDirectors: List[Identity],
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

object StudyProgram {
  implicit def writes: Writes[StudyProgram] = Json.writes
}

case class StudyForm(
    kind: StudyFormType,
    workloadPerEcts: Int,
    scope: List[StudyFormScope]
)

object StudyForm {
  implicit def writes: Writes[StudyForm] = Json.writes
}

case class StudyFormScope(
    programDuration: Int,
    totalEcts: Int,
    deReason: String,
    enReason: String
)

object StudyFormScope {
  implicit def writes: Writes[StudyFormScope] = Json.writes
}

case class RestrictedAdmission(
    value: Boolean,
    deReason: String,
    enReason: String
)

object RestrictedAdmission {
  implicit def writes: Writes[RestrictedAdmission] = Json.writes
}
