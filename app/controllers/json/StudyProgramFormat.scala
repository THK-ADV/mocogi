package controllers.json

import basedata.StudyProgram
import play.api.libs.json.{Format, Json}

trait StudyProgramFormat {
  implicit val studyProgramFormat: Format[StudyProgram] =
    Json.format[StudyProgram]
}
