package controllers.json

import basedata.{FocusArea, StudyProgramPreview}
import play.api.libs.json.{Format, Json}

trait FocusAreaFormat {

  implicit val studyProgramPreview: Format[StudyProgramPreview] =
    Json.format[StudyProgramPreview]

  implicit val focusAreaFormat: Format[FocusArea] =
    Json.format[FocusArea]
}
