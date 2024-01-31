package models

import models.core.RestrictedAdmission
import play.api.libs.json.{Json, Writes}

import java.time.LocalDate
import java.util.UUID

case class StudyProgramOutput(
    id: String,
    deLabel: String,
    enLabel: String,
    internalAbbreviation: String,
    externalAbbreviation: String,
    deUrl: String,
    enUrl: String,
    grade: String,
    programDirectors: List[String],
    examDirectors: List[String],
    accreditationUntil: LocalDate,
    restrictedAdmission: RestrictedAdmission,
    studyForm: List[UUID],
    language: List[String],
    seasons: List[String],
    campus: List[String],
    deDescription: String,
    deNote: String,
    enDescription: String,
    enNote: String
)

object StudyProgramOutput {
  implicit def writes: Writes[StudyProgramOutput] = Json.writes
}
