package controllers.json

import basedata.{RestrictedAdmission, StudyForm, StudyFormScope, StudyProgram}
import database.repo.StudyProgramOutput
import play.api.libs.json.{Format, Json}

trait StudyProgramFormat
    extends GradesFormat
    with PersonFormat
    with StudyFormTypeFormat
    with LanguageFormat
    with SeasonFormat
    with LocationFormat {

  implicit val restrictedAdmissionFormat: Format[RestrictedAdmission] =
    Json.format[RestrictedAdmission]

  implicit val studyFormScopeFormat: Format[StudyFormScope] =
    Json.format[StudyFormScope]

  implicit val studyFormFormat: Format[StudyForm] =
    Json.format[StudyForm]

  implicit val studyProgramFormat: Format[StudyProgram] =
    Json.format[StudyProgram]

  implicit val studyProgramOutputFormat: Format[StudyProgramOutput] =
    Json.format[StudyProgramOutput]
}
