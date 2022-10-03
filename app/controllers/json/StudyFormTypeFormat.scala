package controllers.json

import basedata.StudyFormType
import play.api.libs.json.{Format, Json}

trait StudyFormTypeFormat {
  implicit val studyFormTypeFormat: Format[StudyFormType] =
    Json.format[StudyFormType]
}
