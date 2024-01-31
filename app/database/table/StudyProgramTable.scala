package database.table

import models.core.RestrictedAdmission
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDate

case class StudyProgramDbEntry(
    id: String,
    deLabel: String,
    enLabel: String,
    internalAbbreviation: String,
    externalAbbreviation: String,
    deUrl: String,
    enUrl: String,
    grade: String,
    accreditationUntil: LocalDate,
    restrictedAdmission: RestrictedAdmission,
    deDescription: String,
    deNote: String,
    enDescription: String,
    enNote: String
)

final class StudyProgramTable(tag: Tag)
    extends Table[StudyProgramDbEntry](tag, "study_program")
    with IDLabelColumn[StudyProgramDbEntry] {

  def internalAbbreviation = column[String]("internal_abbreviation")
  def externalAbbreviation = column[String]("external_abbreviation")
  def deUrl = column[String]("de_url")
  def enUrl = column[String]("en_url")
  def grade = column[String]("grade")
  def accreditationUntil = column[LocalDate]("accreditation_until")
  def restrictedAdmissionValue = column[Boolean]("restricted_admission_value")
  def restrictedAdmissionDeReason =
    column[String]("restricted_admission_de_reason")
  def restrictedAdmissionEnReason =
    column[String]("restricted_admission_en_reason")
  def deDescription = column[String]("de_description")
  def deNote = column[String]("de_note")
  def enDescription = column[String]("en_description")
  def enNote = column[String]("en_note")

  def gradeFk =
    foreignKey("grade", grade, TableQuery[GradeTable])(_.id)

  override def * =
    (
      id,
      deLabel,
      enLabel,
      internalAbbreviation,
      externalAbbreviation,
      deUrl,
      enUrl,
      grade,
      accreditationUntil,
      restrictedAdmissionValue,
      restrictedAdmissionDeReason,
      restrictedAdmissionEnReason,
      deDescription,
      deNote,
      enDescription,
      enNote
    ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          LocalDate,
          Boolean,
          String,
          String,
          String,
          String,
          String,
          String
      )
  ) => StudyProgramDbEntry = {
    case (
          abbrev,
          deLabel,
          enLabel,
          internalAbbreviation,
          externalAbbreviation,
          deUrl,
          enUrl,
          grade,
          accreditationUntil,
          restrictedAdmissionValue,
          restrictedAdmissionDeReason,
          restrictedAdmissionEnReason,
          deDescription,
          deNote,
          enDescription,
          enNote
        ) =>
      StudyProgramDbEntry(
        abbrev,
        deLabel,
        enLabel,
        internalAbbreviation,
        externalAbbreviation,
        deUrl,
        enUrl,
        grade,
        accreditationUntil,
        RestrictedAdmission(
          restrictedAdmissionValue,
          restrictedAdmissionDeReason,
          restrictedAdmissionEnReason
        ),
        deDescription,
        deNote,
        enDescription,
        enNote
      )
  }

  def unmapRow: StudyProgramDbEntry => Option[
    (
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        LocalDate,
        Boolean,
        String,
        String,
        String,
        String,
        String,
        String
    )
  ] = { a =>
    Option(
      (
        a.id,
        a.deLabel,
        a.enLabel,
        a.internalAbbreviation,
        a.externalAbbreviation,
        a.deUrl,
        a.enUrl,
        a.grade,
        a.accreditationUntil,
        a.restrictedAdmission.value,
        a.restrictedAdmission.deReason,
        a.restrictedAdmission.enReason,
        a.deDescription,
        a.deNote,
        a.enDescription,
        a.enNote
      )
    )
  }
}
