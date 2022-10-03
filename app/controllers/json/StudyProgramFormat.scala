package controllers.json

import basedata.StudyProgramWithPO
import play.api.libs.json.{Format, Json}

trait StudyProgramFormat {
  implicit val studyProgramFormat: Format[StudyProgramWithPO] =
    Json.format[StudyProgramWithPO]
}
